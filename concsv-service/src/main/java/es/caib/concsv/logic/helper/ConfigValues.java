package es.caib.concsv.logic.helper;

import es.caib.concsv.commons.config.PropertyFileConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lectura, des dels serveis CDI, de les propietats que es poden canviar des del backoffice
 * (les que tenen {@code JBOSS_PROPERTY = 0} a la taula {@code CSV_CONFIG}).
 * <p>
 * Els serveis CDI no poden injectar el {@code ConfigHelper} (és un bean de Spring) i amb
 * {@code @ConfigProperty} el valor es llegeix una sola vegada. Per això aquestes propietats es
 * llegeixen a cada ús, com fan {@code ConfigHelper.getConfig} a Ripea i Distribucio. L'ordre és:
 * <ol>
 * <li>el valor de la base de dades, si n'hi ha;</li>
 * <li>el fitxer de propietats del servidor (MicroProfile Config);</li>
 * <li>el valor per defecte indicat pel qui crida.</li>
 * </ol>
 * Si la base de dades no es pot consultar (taula inexistent, datasource desactivat) es fa servir
 * el fitxer, com abans de tenir la pantalla de propietats.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@ApplicationScoped
public class ConfigValues {

	static final String DATASOURCE_JNDI = "java:jboss/datasources/concsvDS";
	static final String SQL = "SELECT VALUE FROM CSV_CONFIG WHERE KEY = ? AND JBOSS_PROPERTY = 0";
	static final String SQL_TOTES = "SELECT KEY, VALUE FROM CSV_CONFIG WHERE JBOSS_PROPERTY = 0 AND VALUE IS NOT NULL";

	/**
	 * Valors que es consideren certs a les propietats booleanes (sense distingir majúscules):
	 * els de MicroProfile Config més la {@code S} que usen algunes propietats de ConCSV.
	 */
	private static final Set<String> VALORS_CERTS = Set.of("true", "s", "1", "yes", "y", "on");

	private final AtomicBoolean errorAvisat = new AtomicBoolean(false);
	private volatile DataSource dataSource;

	/** Valor de la propietat, o {@code null} si no està definida enlloc. */
	public String get(String key) {
		return get(key, null);
	}

	/** Valor de la propietat, o el valor per defecte si no està definida o és buida. */
	public String get(String key, String defaultValue) {
		String value = getFromDatabase(key);
		if (value == null || value.isEmpty()) {
			value = getFromFile(key);
		}
		return value == null || value.isEmpty() ? defaultValue : value;
	}

	/** Valor booleà de la propietat (veure {@link #isTrue(String)}). */
	public boolean getBoolean(String key, boolean defaultValue) {
		String value = get(key);
		return value == null || value.trim().isEmpty() ? defaultValue : isTrue(value);
	}

	/** Valor enter de la propietat; si no és un enter vàlid es retorna el valor per defecte. */
	public int getInt(String key, int defaultValue) {
		long value = getLong(key, defaultValue);
		return value > Integer.MAX_VALUE || value < Integer.MIN_VALUE ? defaultValue : (int) value;
	}

	/** Valor enter llarg de la propietat; si no és un enter vàlid es retorna el valor per defecte. */
	public long getLong(String key, long defaultValue) {
		String value = get(key);
		if (value == null || value.trim().isEmpty()) {
			return defaultValue;
		}
		try {
			return Long.parseLong(value.trim());
		} catch (NumberFormatException ex) {
			log.warn("El valor de la propietat {} no és un enter; s'usa el valor per defecte {}", key, defaultValue);
			return defaultValue;
		}
	}

	/**
	 * Indica si un valor representa cert: {@code true}, {@code s}, {@code 1}, {@code yes},
	 * {@code y} o {@code on}, sense distingir majúscules. Qualsevol altre valor (inclòs
	 * {@code null}) és fals.
	 */
	public static boolean isTrue(String value) {
		return value != null && VALORS_CERTS.contains(value.trim().toLowerCase());
	}

	/**
	 * Propietats per als plugins: les del fitxer de propietats del servidor, amb les propietats
	 * editables que tenen valor a la base de dades per damunt. Es crea un objecte nou a cada crida.
	 */
	public Properties getProperties() {
		Properties properties = new Properties();
		try {
			properties.putAll(PropertyFileConfigUtil.getProperties());
		} catch (Throwable ex) {
			log.debug("No s'han pogut llegir les propietats del fitxer: {}", ex.toString());
		}
		try {
			DataSource ds = getDataSource();
			try (Connection connection = ds.getConnection();
					PreparedStatement statement = connection.prepareStatement(SQL_TOTES);
					ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					String value = rs.getString(2);
					if (value != null && !value.isEmpty()) {
						properties.put(rs.getString(1), value);
					}
				}
			}
		} catch (Exception ex) {
			avisaError("les propietats", ex);
		}
		return properties;
	}

	/** Valor de la base de dades, o {@code null} si no n'hi ha o no es pot consultar. */
	private String getFromDatabase(String key) {
		try {
			DataSource ds = getDataSource();
			try (Connection connection = ds.getConnection();
					PreparedStatement statement = connection.prepareStatement(SQL)) {
				statement.setString(1, key);
				try (ResultSet rs = statement.executeQuery()) {
					return rs.next() ? rs.getString(1) : null;
				}
			}
		} catch (Exception ex) {
			// No s'ha de trencar mai el servei per la configuració: es fa servir el fitxer.
			avisaError("la propietat " + key, ex);
			return null;
		}
	}

	/** Avisa (WARN) del primer error en llegir de la base de dades; els següents només a DEBUG. */
	private void avisaError(String que, Exception ex) {
		if (errorAvisat.compareAndSet(false, true)) {
			log.warn("No s'ha pogut llegir {} de la base de dades; s'usa el fitxer de propietats: {}", que, ex.toString());
		} else {
			log.debug("No s'ha pogut llegir {} de la base de dades: {}", que, ex.toString());
		}
	}

	private DataSource getDataSource() throws Exception {
		DataSource ds = dataSource;
		if (ds == null) {
			ds = (DataSource) new InitialContext().lookup(DATASOURCE_JNDI);
			dataSource = ds;
		}
		return ds;
	}

	private String getFromFile(String key) {
		try {
			return ConfigProvider.getConfig().getOptionalValue(key, String.class).orElse(null);
		} catch (Throwable ex) {
			// Sense MicroProfile Config (tests, mode standalone) o sense les propietats de sistema
			// que carreguen els fitxers: com si la propietat no hi fos.
			log.debug("No s'ha pogut llegir la propietat {} amb MicroProfile Config: {}", key, ex.toString());
			return null;
		}
	}

	/** Només per a tests: datasource alternatiu al de JNDI. */
	void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

}
