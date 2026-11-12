package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import es.caib.concsv.logic.intf.config.PropertyConfig;

/**
 * Reproducció de la caiguda de la BBDD de PRO del 06/11/2026 (pool de {@code concsvDS} exhaurit,
 * IJ000453/IJ000655, i timer de {@code HashServiceEjb} solapat, WFLYEJB0043).
 * <p>
 * Es compten les connexions que es demanen al datasource. A la 2.2.1 cada lectura d'una propietat
 * fa una consulta a {@code CSV_CONFIG}, i la neteja de la memòria cau en fa dues per fitxer cada
 * minut. Aquests tests fixen que el nombre de consultes no creixi amb les peticions ni amb els
 * fitxers de la memòria cau.
 */
public class ConsultesConfigBdTest {

	/** Propietats que llegeix una consulta de document (HashService, PerformanceInterceptor, OldSaveKeepingService). */
	private static final String[] PROPIETATS_PER_PETICIO = {
			PropertyConfig.PROP_CACHE_ACTIVA,
			PropertyConfig.PROP_CONSULT_NEW_DIGITAL_ARCHIVE,
			PropertyConfig.PROP_CONSULT_OLD_SAFEKEEPING,
			PropertyConfig.PROP_LOGO_PATH,
			PropertyConfig.PROP_PERFORMANCE,
			PropertyConfig.PROP_OLD_SAVEKEEPING_TIMEOUT,
	};
	private static final int PETICIONS = 100;
	private static final int FITXERS_CACHE = 1000;

	private DataSource dataSource;
	private ConfigValues configValues;
	private Path fitxersPath;

	@Before
	public void setUp() throws Exception {
		dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		ResultSet resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		// Cap propietat amb valor a la BBDD: es fan servir els valors per defecte
		when(resultSet.next()).thenReturn(false);
		configValues = spy(new ConfigValues());
		configValues.setDataSource(dataSource);
		fitxersPath = Files.createTempDirectory("concsv-consultes-test");
	}

	@After
	public void tearDown() throws IOException {
		try (Stream<Path> paths = Files.walk(fitxersPath)) {
			List<Path> ordenats = paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
			for (Path path: ordenats) {
				Files.deleteIfExists(path);
			}
		}
	}

	@Test
	public void configValues_lesPeticionsNoConsultenLaBdACadaLectura() {
		for (int i = 0; i < PETICIONS; i++) {
			for (String propietat: PROPIETATS_PER_PETICIO) {
				configValues.get(propietat);
			}
		}

		long connexions = connexionsDemanades();
		assertTrue(
				String.format("%d peticions han fet %d consultes a CSV_CONFIG (%d per petició); se n'esperava com a molt 1",
						PETICIONS, connexions, connexions / PETICIONS),
				connexions <= 1);
	}

	@Test
	public void cleanExpired_lesConsultesNoCreixenAmbElsFitxersDeLaCache() throws IOException {
		CacheHelper cacheHelper = cacheHelperAmbFitxers(FITXERS_CACHE);

		cacheHelper.cleanExpired();

		long connexions = connexionsDemanades();
		assertTrue(
				String.format("Netejar una memòria cau de %d fitxers ha fet %d consultes a CSV_CONFIG; se n'esperava com a molt 1",
						FITXERS_CACHE, connexions),
				connexions <= 1);
	}

	@Test
	public void cleanExpired_llegeixElTtlUnaSolaVegada() throws IOException {
		CacheHelper cacheHelper = cacheHelperAmbFitxers(FITXERS_CACHE);

		cacheHelper.cleanExpired();

		long lectures = mockingDetails(configValues).getInvocations().stream()
				.filter(inv -> inv.getMethod().getName().equals("getLong"))
				.count();
		assertTrue(
				String.format("Netejar una memòria cau de %d fitxers ha llegit %d vegades %s; s'esperava 1",
						FITXERS_CACHE, lectures, PropertyConfig.PROP_CACHE_TTL_MINUTS),
				lectures <= 1);
	}

	/** Memòria cau amb fitxers recents (no expirats) i el TTL llegit de la configuració. */
	private CacheHelper cacheHelperAmbFitxers(int nombre) throws IOException {
		Path info = Files.createDirectories(fitxersPath.resolve("cache").resolve("INFO"));
		for (int i = 0; i < nombre; i++) {
			Files.write(info.resolve("CSV_" + i), new byte[] { 0 });
		}
		CacheHelper cacheHelper = new CacheHelper();
		cacheHelper.setFitxersPath(fitxersPath.toString());
		cacheHelper.setConfigValues(configValues);
		return cacheHelper;
	}

	private long connexionsDemanades() {
		return mockingDetails(dataSource).getInvocations().stream()
				.filter(inv -> inv.getMethod().getName().equals("getConnection"))
				.count();
	}

}
