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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Lectura, des dels serveis CDI, de les propietats que es poden canviar des del backoffice
 * (les que tenen {@code JBOSS_PROPERTY = 0} a la taula {@code CSV_CONFIG}).
 * <p>
 * Els serveis CDI no poden injectar el {@code ConfigHelper} (és un bean de Spring) i amb
 * {@code @ConfigProperty} el valor es llegeix una sola vegada. L'ordre és:
 * <ol>
 * <li>el valor de la base de dades, si n'hi ha;</li>
 * <li>el fitxer de propietats del servidor (MicroProfile Config);</li>
 * <li>el valor per defecte indicat pel qui crida.</li>
 * </ol>
 * Els valors de la base de dades es llegeixen tots d'un cop i es guarden en memòria. Es tornen a
 * llegir:
 * <ul>
 * <li>quan es modifica una propietat des del backoffice, un cop fet el commit ({@link #invalida()});
 * només afecta el node que ha desat el canvi;</li>
 * <li>cada {@link #DURADA_CACHE_MS} mil·lisegons, perquè els altres nodes i els canvis fets
 * directament a la base de dades (scripts) també s'apliquin.</li>
 * </ul>
 * Abans (2.2.1) es feia una consulta a cada lectura i, amb molta càrrega, això exhauria el pool de
 * {@code concsvDS} (caiguda de PRO del 06/11/2026).
 * <p>
 * Si la base de dades no es pot consultar (taula inexistent, datasource desactivat, pool exhaurit)
 * es fa servir el fitxer, com abans de tenir la pantalla de propietats, i no es torna a provar fins
 * que passa {@link #DURADA_REINTENT_MS}: així cap lectura no espera el temps de bloqueig del pool.
 * Si ja hi havia valors carregats, es continuen fent servir.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@ApplicationScoped
public class ConfigValues {

	static final String DATASOURCE_JNDI = "java:jboss/datasources/concsvDS";
	static final String SQL_TOTES = "SELECT KEY, VALUE, TYPE_CODE FROM CSV_CONFIG WHERE JBOSS_PROPERTY = 0 AND VALUE IS NOT NULL";
	/** Tipus de les propietats amb credencials: el seu valor no surt mai al log. */
	static final String TIPUS_CREDENCIALS = "CREDENTIALS";
	static final String VALOR_EMMASCARAT = "********";

	/** Temps màxim que es guarden en memòria els valors de la base de dades (5 minuts). */
	static final long DURADA_CACHE_MS = 300_000L;

	/**
	 * Generació dels valors de la base de dades: {@link #invalida()} la incrementa i les instàncies
	 * que han carregat una generació anterior tornen a llegir. És estàtica perquè el servei Spring
	 * del backoffice (context Spring de l'EJB) no pot injectar aquest bean CDI; la classe es carrega
	 * un sol cop a la llibreria de l'EAR i per tant és compartida per tots els mòduls.
	 */
	private static final AtomicLong GENERACIO = new AtomicLong();

	/** Temps fins a tornar a provar la base de dades després d'una càrrega fallida (1 minut). */
	static final long DURADA_REINTENT_MS = 60_000L;

	/**
	 * Valors que es consideren certs a les propietats booleanes (sense distingir majúscules):
	 * els de MicroProfile Config més la {@code S} que usen algunes propietats de ConCSV.
	 */
	private static final Set<String> VALORS_CERTS = Set.of("true", "s", "1", "yes", "y", "on");

	private final AtomicBoolean errorAvisat = new AtomicBoolean(false);
	private final ReentrantLock lockCarrega = new ReentrantLock();
	private volatile DataSource dataSource;
	private volatile long duradaCacheMs = DURADA_CACHE_MS;

	/** Valors de la base de dades carregats, o {@code null} si encara no s'han carregat mai. */
	private volatile Map<String, String> valorsBd;
	/** Moment ({@link System#nanoTime()}) de la darrera càrrega, encertada o no. */
	private volatile long darreraCarrega;
	/** Generació ({@link #GENERACIO}) de la darrera càrrega. */
	private volatile long generacioCarregada = -1;
	/** Indica si la darrera càrrega ha fallat (es torna a provar abans, veure {@link #DURADA_REINTENT_MS}). */
	private volatile boolean darreraFallida;

	/**
	 * Fa que la pròxima lectura torni a llegir els valors de la base de dades. S'ha de cridar un
	 * cop fet el commit del canvi: si es crida abans, una lectura concurrent podria tornar a carregar
	 * el valor anterior.
	 */
	public static void invalida() {
		GENERACIO.incrementAndGet();
	}

	/** Generació actual dels valors (veure {@link #invalida()}); per a tests i diagnòstic. */
	public static long getGeneracio() {
		return GENERACIO.get();
	}

	/** Valor de la propietat, o {@code null} si no està definida enlloc. */
	public String get(String key) {
		return get(key, null);
	}

	/** Valor de la propietat, o el valor per defecte si no està definida o és buida. */
	public String get(String key, String defaultValue) {
		String value = getValorsBd().get(key);
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
		properties.putAll(getValorsBd());
		return properties;
	}

	/**
	 * Valors de la base de dades, carregats com a molt un cop cada {@link #DURADA_CACHE_MS} o quan
	 * s'han invalidat ({@link #invalida()}).
	 * <p>
	 * Només un fil fa la càrrega. Mentrestant, si ja hi ha valors carregats, els altres fils els
	 * fan servir en lloc d'esperar; només esperen si encara no se n'ha carregat cap.
	 */
	private Map<String, String> getValorsBd() {
		Map<String, String> valors = valorsBd;
		if (valors != null && !caducat()) {
			return valors;
		}
		if (valors != null) {
			if (!lockCarrega.tryLock()) {
				return valors;
			}
		} else {
			lockCarrega.lock();
		}
		try {
			// Un altre fil pot haver-los carregat mentre esperàvem
			if (valorsBd == null || caducat()) {
				carrega();
			}
			return valorsBd;
		} finally {
			lockCarrega.unlock();
		}
	}

	private boolean caducat() {
		long durada = darreraFallida ? Math.min(duradaCacheMs, DURADA_REINTENT_MS) : duradaCacheMs;
		return generacioCarregada != GENERACIO.get()
				|| System.nanoTime() - darreraCarrega >= durada * 1_000_000L;
	}

	/** Llegeix tots els valors de la base de dades. Si falla, conserva els anteriors (o cap). */
	private void carrega() {
		// Es llegeix abans de consultar: si s'invalida durant la consulta, la pròxima lectura recarrega
		long generacio = GENERACIO.get();
		String motiu = motiuCarrega(generacio);
		try {
			DataSource ds = getDataSource();
			Map<String, String> nous = new HashMap<>();
			Map<String, String> perAlLog = new TreeMap<>();
			try (Connection connection = ds.getConnection();
					PreparedStatement statement = connection.prepareStatement(SQL_TOTES);
					ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					String key = rs.getString(1);
					String value = rs.getString(2);
					if (key != null && value != null && !value.isEmpty()) {
						nous.put(key, value);
						perAlLog.put(key, valorPerAlLog(rs.getString(3), value));
					}
				}
			}
			valorsBd = Collections.unmodifiableMap(nous);
			log.info("Propietats de la base de dades carregades en memòria ({}): {} amb valor {}", motiu, perAlLog.size(), perAlLog);
			darreraFallida = false;
			errorAvisat.set(false);
		} catch (Exception ex) {
			// No s'ha de trencar mai el servei per la configuració: es fa servir el fitxer.
			avisaError(ex);
			darreraFallida = true;
			if (valorsBd == null) {
				valorsBd = Collections.emptyMap();
			}
		} finally {
			darreraCarrega = System.nanoTime();
			generacioCarregada = generacio;
		}
	}

	/** Valor que es mostra al log: el de les credencials s'emmascara. */
	static String valorPerAlLog(String tipus, String value) {
		return TIPUS_CREDENCIALS.equals(tipus) ? VALOR_EMMASCARAT : value;
	}

	/** Motiu de la càrrega, per al log. */
	private String motiuCarrega(long generacio) {
		if (valorsBd == null) {
			return "primera càrrega";
		}
		if (generacioCarregada != generacio) {
			return "propietats modificades des del backoffice";
		}
		if (darreraFallida) {
			return "reintent després d'un error";
		}
		return "recàrrega periòdica cada " + (duradaCacheMs / 60_000L) + " min";
	}

	/** Avisa (WARN) del primer error en llegir de la base de dades; els següents només a DEBUG. */
	private void avisaError(Exception ex) {
		if (errorAvisat.compareAndSet(false, true)) {
			log.warn("No s'han pogut llegir les propietats de la base de dades; s'usen els valors anteriors o el fitxer de propietats: {}", ex.toString());
		} else {
			log.debug("No s'han pogut llegir les propietats de la base de dades: {}", ex.toString());
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

	/** Només per a tests: temps que es guarden els valors (0 = es tornen a llegir a cada ús). */
	void setDuradaCacheMs(long duradaCacheMs) {
		this.duradaCacheMs = duradaCacheMs;
	}

}
