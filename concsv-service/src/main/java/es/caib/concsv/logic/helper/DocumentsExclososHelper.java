package es.caib.concsv.logic.helper;

import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Consulta, des dels serveis CDI, si un document està exclòs de la descàrrega de l'original, amb la
 * taula {@code CSV_DOCUMENT_EXCLOS} que es manté des del backoffice.
 * <p>
 * Com {@link ConfigValues}, llegeix la base de dades per JDBC a cada ús (els serveis CDI no veuen els
 * repositoris de Spring) i no en guarda cap còpia: un canvi del backoffice val a l'instant. Si la base
 * de dades no es pot consultar, el document es considera exclòs: és preferible no deixar descarregar
 * un original que podria estar prohibit.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@ApplicationScoped
public class DocumentsExclososHelper {

	static final String SQL_PREFIX = "SELECT 1 FROM CSV_DOCUMENT_EXCLOS WHERE VALOR IN (";

	@Inject
	private SubsistemesHelper subsistemesHelper;

	private final AtomicBoolean errorAvisat = new AtomicBoolean(false);
	private volatile DataSource dataSource;

	/**
	 * Indica si algun dels identificadors (UUID o CSV del document) és a la llista d'exclosos. Els
	 * identificadors nuls o buits s'ignoren; si no en queda cap, el resultat és fals.
	 */
	public boolean isExclos(String... identificadors) {
		List<String> valors = identificadors == null ? List.of() : Arrays.stream(identificadors).
				filter(Objects::nonNull).
				map(String::trim).
				filter(v -> !v.isEmpty()).
				distinct().
				collect(Collectors.toList());
		if (valors.isEmpty()) {
			return false;
		}
		long t0 = System.currentTimeMillis();
		try {
			boolean exclos = consulta(valors);
			registra(t0, false);
			return exclos;
		} catch (Exception ex) {
			registra(t0, true);
			avisaError(ex);
			return true;
		}
	}

	private boolean consulta(List<String> valors) throws Exception {
		String sql = SQL_PREFIX + valors.stream().map(v -> "?").collect(Collectors.joining(", ")) + ")";
		DataSource ds = getDataSource();
		try (Connection connection = ds.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {
			for (int i = 0; i < valors.size(); i++) {
				statement.setString(i + 1, valors.get(i));
			}
			try (ResultSet rs = statement.executeQuery()) {
				return rs.next();
			}
		}
	}

	private void registra(long t0, boolean error) {
		if (subsistemesHelper != null) {
			subsistemesHelper.addOperation(SubsistemesHelper.SubsistemesEnum.EXC, t0, error);
		}
	}

	/** Avisa (WARN) del primer error en llegir de la base de dades; els següents només a DEBUG. */
	private void avisaError(Exception ex) {
		if (errorAvisat.compareAndSet(false, true)) {
			log.warn("No s'ha pogut consultar la taula de documents exclosos; es consideren exclosos: {}", ex.toString());
		} else {
			log.debug("No s'ha pogut consultar la taula de documents exclosos: {}", ex.toString());
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
	void setSubsistemesHelper(SubsistemesHelper subsistemesHelper) {
		this.subsistemesHelper = subsistemesHelper;
	}

}
