package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.logic.intf.model.IntegracioAccioTipusEnum;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.persist.entity.MapToJsonConverter;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Escriu a la taula {@code CSV_INTEGRACIO_ACCIO} (monitor d'integracions del backoffice) les
 * peticions que l'aplicació fa a les integracions externes.
 * <p>
 * Les peticions s'encuen en memòria des del camí públic de consulta, que és d'alta concurrència, i
 * un procés programat les desa en lots per JDBC (com {@link ConfigValues} i
 * {@link DocumentsExclososHelper}, perquè els serveis CDI no veuen els repositoris de Spring).
 * <p>
 * Aquesta classe <b>no pot fer fallar mai</b> la consulta pública: encuar no bloqueja (si la cua és
 * plena es descarta la petició) i qualsevol error de base de dades es registra com a avís i es
 * descarta el lot. Es perd el registre, no la petició.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@ApplicationScoped
public class MonitorIntegracioHelper {

	/** Nombre màxim de peticions pendents de desar. Les que no hi caben es descarten. */
	static final int CAPACITAT_CUA = 5000;
	/** Files que esborra com a màxim cada sentència de neteja. */
	static final int LOT_ESBORRAT = 1000;
	/** Usuari amb el qual es desen els registres (CREATEDBY_CODI). */
	static final String USUARI = "concsv";

	// Mides de les columnes de CSV_INTEGRACIO_ACCIO.
	static final int MIDA_DESCRIPCIO = 1024;
	static final int MIDA_ENDPOINT = 1024;
	static final int MIDA_ERROR = 2000;
	/** Límit propi de la traça (la columna és un CLOB): n'hi ha prou per veure la causa. */
	static final int MIDA_TRACA = 8000;

	static final String SQL_INSERT = "INSERT INTO CSV_INTEGRACIO_ACCIO "
			+ "(ID, DATA, INTEGRACIO_CODI, DESCRIPCIO, ENDPOINT, PARAMETRES, TIPUS, TEMPS_RESPOSTA, ESTAT, "
			+ "ERROR_DESCRIPCIO, EXCEPCIO_MESSAGE, EXCEPCIO_STACKTRACE, CREATEDBY_CODI, CREATEDDATE) "
			+ "VALUES (CSV_HIBERNATE_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	static final String SQL_ESBORRA = "DELETE FROM CSV_INTEGRACIO_ACCIO WHERE DATA < ? AND ROWNUM <= " + LOT_ESBORRAT;

	/** Petició a una integració externa pendent de desar. */
	@Getter
	@Builder
	public static class Registre {
		private final Date data;
		private final IntegracioCodiEnum codi;
		private final String descripcio;
		private final String endpoint;
		private final Map<String, String> parametres;
		private final IntegracioAccioTipusEnum tipus;
		private final long tempsResposta;
		private final IntegracioAccioEstatEnum estat;
		private final String errorDescripcio;
		private final String excepcioMessage;
		private final String excepcioStacktrace;
	}

	@Inject
	private ConfigValues configValues;

	private final BlockingQueue<Registre> cua = new ArrayBlockingQueue<>(CAPACITAT_CUA);
	private final AtomicLong descartats = new AtomicLong();
	private final AtomicBoolean errorAvisat = new AtomicBoolean(false);
	private final MapToJsonConverter jsonConverter = new MapToJsonConverter();
	/**
	 * Interruptor de la propietat {@code integracio.monitor.actiu}. Es refresca només a cada buidatge:
	 * llegir-lo a cada petició suposaria una consulta SQL al camí públic.
	 */
	private volatile boolean actiu = true;
	private volatile DataSource dataSource;

	/**
	 * Encua una petició per desar-la en el proper buidatge. No bloqueja mai: si el monitor està
	 * desactivat o la cua és plena, la petició es descarta.
	 */
	public void encua(Registre registre) {
		if (registre == null || !actiu) {
			return;
		}
		if (!cua.offer(registre)) {
			descartats.incrementAndGet();
		}
	}

	/**
	 * Desa en un sol lot les peticions pendents.
	 *
	 * @return el nombre de peticions desades.
	 */
	public int flush() {
		refrescaInterruptor();
		if (!actiu) {
			cua.clear();
			return 0;
		}
		long perdudes = descartats.getAndSet(0);
		if (perdudes > 0) {
			log.warn("El monitor d'integracions ha descartat {} peticions perquè la cua era plena", perdudes);
		}
		List<Registre> registres = new ArrayList<>();
		cua.drainTo(registres, CAPACITAT_CUA);
		if (registres.isEmpty()) {
			return 0;
		}
		try {
			insereix(registres);
			errorAvisat.set(false);
			return registres.size();
		} catch (Exception ex) {
			avisaError("desar " + registres.size() + " peticions", ex);
			return 0;
		}
	}

	/**
	 * Esborra les peticions anteriors al nombre de dies indicat, en lots perquè no calgui un
	 * {@code undo} gran. Un nombre de dies no positiu no esborra res (mai tot).
	 *
	 * @return el nombre de peticions esborrades.
	 */
	public int esborraAntics(int dies) {
		if (dies <= 0) {
			return 0;
		}
		Timestamp limit = Timestamp.valueOf(LocalDateTime.now().minusDays(dies));
		int total = 0;
		try {
			DataSource ds = getDataSource();
			try (Connection connection = ds.getConnection();
					PreparedStatement statement = connection.prepareStatement(SQL_ESBORRA)) {
				int esborrades;
				do {
					statement.setTimestamp(1, limit);
					esborrades = statement.executeUpdate();
					total += esborrades;
				} while (esborrades >= LOT_ESBORRAT);
			}
			errorAvisat.set(false);
		} catch (Exception ex) {
			avisaError("esborrar les peticions antigues", ex);
		}
		return total;
	}

	private void insereix(List<Registre> registres) throws Exception {
		DataSource ds = getDataSource();
		Timestamp ara = new Timestamp(System.currentTimeMillis());
		try (Connection connection = ds.getConnection();
				PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {
			for (Registre registre : registres) {
				Date data = registre.getData() != null ? registre.getData() : new Date();
				statement.setTimestamp(1, new Timestamp(data.getTime()));
				statement.setString(2, registre.getCodi().name());
				setText(statement, 3, registre.getDescripcio(), MIDA_DESCRIPCIO);
				setText(statement, 4, registre.getEndpoint(), MIDA_ENDPOINT);
				setClob(statement, 5, jsonConverter.convertToDatabaseColumn(registre.getParametres()), Integer.MAX_VALUE);
				setText(statement, 6, registre.getTipus() != null ? registre.getTipus().name() : null, 32);
				statement.setLong(7, registre.getTempsResposta());
				statement.setString(8, registre.getEstat().name());
				setText(statement, 9, registre.getErrorDescripcio(), MIDA_ERROR);
				setText(statement, 10, registre.getExcepcioMessage(), MIDA_ERROR);
				setClob(statement, 11, registre.getExcepcioStacktrace(), MIDA_TRACA);
				statement.setString(12, USUARI);
				statement.setTimestamp(13, ara);
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private static String trunca(String valor, int mida) {
		return valor != null && valor.length() > mida ? valor.substring(0, mida) : valor;
	}

	private static void setText(PreparedStatement statement, int index, String valor, int mida) throws Exception {
		String text = trunca(valor, mida);
		if (text == null) {
			statement.setNull(index, Types.VARCHAR);
		} else {
			statement.setString(index, text);
		}
	}

	private static void setClob(PreparedStatement statement, int index, String valor, int mida) throws Exception {
		String text = trunca(valor, mida);
		if (text == null) {
			statement.setNull(index, Types.CLOB);
		} else {
			statement.setCharacterStream(index, new StringReader(text), text.length());
		}
	}

	private void refrescaInterruptor() {
		try {
			actiu = configValues == null || configValues.getBoolean(PropertyConfig.PROP_INTEGRACIO_MONITOR_ACTIU, true);
		} catch (Exception ex) {
			// Es conserva l'últim valor conegut.
			log.debug("No s'ha pogut llegir la propietat del monitor d'integracions: {}", ex.toString());
		}
	}

	/** Avisa (WARN) del primer error de base de dades; els següents només a DEBUG. */
	private void avisaError(String que, Exception ex) {
		if (errorAvisat.compareAndSet(false, true)) {
			log.warn("El monitor d'integracions no ha pogut {}: {}", que, ex.toString());
		} else {
			log.debug("El monitor d'integracions no ha pogut {}: {}", que, ex.toString());
		}
	}

	private DataSource getDataSource() throws Exception {
		DataSource ds = dataSource;
		if (ds == null) {
			ds = (DataSource) new InitialContext().lookup(ConfigValues.DATASOURCE_JNDI);
			dataSource = ds;
		}
		return ds;
	}

	/** Només per a tests: datasource alternatiu al de JNDI. */
	void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/** Només per a tests. */
	void setConfigValues(ConfigValues configValues) {
		this.configValues = configValues;
	}

	/** Només per a tests: peticions pendents de desar. */
	int pendents() {
		return cua.size();
	}

}
