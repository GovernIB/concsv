package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests de {@link ConfigValues}: lectura del valor de la base de dades, caiguda al valor per defecte
 * si la base de dades falla o no el té, conversió de tipus i memòria cau dels valors. Sense
 * MicroProfile Config a les proves, la propietat "del fitxer" no hi és mai.
 */
public class ConfigValuesTest {

	private ConfigValues configValues;
	private DataSource dataSource;
	private Connection connection;
	/** Files que retorna la consulta: parelles clau/valor. */
	private final List<String[]> files = new ArrayList<>();
	private ExecutorService executor;

	@Before
	public void setUp() throws Exception {
		dataSource = mock(DataSource.class);
		connection = mock(Connection.class);
		PreparedStatement statement = mock(PreparedStatement.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenAnswer(inv -> resultSet(new ArrayList<>(files)));
		configValues = new ConfigValues();
		configValues.setDataSource(dataSource);
		// Per defecte es torna a llegir a cada ús, perquè els tests puguin canviar els valors
		configValues.setDuradaCacheMs(0);
	}

	@After
	public void tearDown() {
		if (executor != null) {
			executor.shutdownNow();
		}
	}

	/** La consulta retorna aquestes parelles clau/valor. */
	private void bdRetorna(String... clausIValors) {
		files.clear();
		for (int i = 0; i < clausIValors.length; i += 2) {
			files.add(new String[] { clausIValors[i], clausIValors[i + 1] });
		}
	}

	private static ResultSet resultSet(List<String[]> files) throws SQLException {
		ResultSet rs = mock(ResultSet.class);
		int[] fila = { -1 };
		when(rs.next()).thenAnswer(inv -> ++fila[0] < files.size());
		when(rs.getString(1)).thenAnswer(inv -> files.get(fila[0])[0]);
		when(rs.getString(2)).thenAnswer(inv -> files.get(fila[0])[1]);
		return rs;
	}

	@Test
	public void get_prenElValorDeLaBd() {
		bdRetorna("clau.a", "valor-bd", "clau.b", "altre");

		assertEquals("valor-bd", configValues.get("clau.a"));
		assertEquals("altre", configValues.get("clau.b"));
	}

	@Test
	public void get_consultaNomesLesPropietatsEditables() {
		assertTrue(ConfigValues.SQL_TOTES.contains("JBOSS_PROPERTY = 0"));
	}

	@Test
	public void get_senseFilaOAmbValorNulRetornaElPerDefecte() {
		bdRetorna();
		assertNull(configValues.get("clau.a"));
		assertEquals("defecte", configValues.get("clau.a", "defecte"));

		bdRetorna("clau.a", null);
		assertEquals("defecte", configValues.get("clau.a", "defecte"));

		bdRetorna("clau.a", "");
		assertEquals("defecte", configValues.get("clau.a", "defecte"));
	}

	@Test
	public void get_siLaBdFallaRetornaElPerDefecteSenseLlancarExcepcio() throws Exception {
		when(dataSource.getConnection()).thenThrow(new SQLException("ORA-00942"));

		assertEquals("defecte", configValues.get("clau.a", "defecte"));
		assertNull(configValues.get("clau.a"));
	}

	@Test
	public void getProperties_incloLesPropietatsEditablesAmbValorDeLaBd() {
		bdRetorna("clau.a", "valor-a", "clau.b", "");

		java.util.Properties properties = configValues.getProperties();

		assertEquals("valor-a", properties.getProperty("clau.a"));
		// Un valor buit no substitueix el del fitxer
		assertNull(properties.getProperty("clau.b"));
	}

	@Test
	public void getProperties_siLaBdFallaNoLlancaExcepcio() throws Exception {
		when(dataSource.getConnection()).thenThrow(new SQLException("ORA-00942"));

		assertNull(configValues.getProperties().getProperty("clau.a"));
	}

	@Test
	public void get_senseDatasourceJndiRetornaElPerDefecte() {
		// Sense JNDI (tests) la consulta falla i es fa servir el fitxer, que aquí no hi és
		assertEquals("defecte", new ConfigValues().get("clau.a", "defecte"));
	}

	@Test
	public void isTrue_accepta_true_S_1_yes_y_on_sensePerMajuscules() {
		for (String valor : new String[] { "true", "TRUE", "S", "s", "1", "yes", "Y", "on", " S " }) {
			assertTrue(valor, ConfigValues.isTrue(valor));
		}
		for (String valor : new String[] { "false", "N", "n", "0", "no", "off", "SN", "", "  " }) {
			assertFalse(valor, ConfigValues.isTrue(valor));
		}
		assertFalse(ConfigValues.isTrue(null));
	}

	@Test
	public void getBoolean_ambSiNoITrueFalse() {
		bdRetorna("clau.a", "S");
		assertTrue(configValues.getBoolean("clau.a", false));
		bdRetorna("clau.a", "N");
		assertFalse(configValues.getBoolean("clau.a", true));
		bdRetorna("clau.a", "true");
		assertTrue(configValues.getBoolean("clau.a", false));
		bdRetorna("clau.a", "false");
		assertFalse(configValues.getBoolean("clau.a", true));
	}

	@Test
	public void getBoolean_senseValorRetornaElPerDefecte() {
		bdRetorna();
		assertTrue(configValues.getBoolean("clau.a", true));
		assertFalse(configValues.getBoolean("clau.a", false));
	}

	@Test
	public void getInt_iGetLong_enterValid() {
		bdRetorna("clau.a", " 45 ");
		assertEquals(45, configValues.getInt("clau.a", 1));
		assertEquals(45L, configValues.getLong("clau.a", 1L));
	}

	@Test
	public void getInt_iGetLong_enterInvalidORUnaBdBuidaRetornenElPerDefecte() {
		bdRetorna("clau.a", "abc");
		assertEquals(30, configValues.getInt("clau.a", 30));
		assertEquals(30L, configValues.getLong("clau.a", 30L));

		bdRetorna();
		assertEquals(365, configValues.getInt("clau.a", 365));
	}

	@Test
	public void getInt_siNoCapAInteger_retornaElPerDefecte() {
		bdRetorna("clau.a", "99999999999");
		assertEquals(7, configValues.getInt("clau.a", 7));
		assertEquals(99999999999L, configValues.getLong("clau.a", 1L));
	}

	@Test
	public void cache_dinsLaDuradaNoEsTornaAConsultarLaBd() throws Exception {
		configValues.setDuradaCacheMs(ConfigValues.DURADA_CACHE_MS);
		bdRetorna("clau.a", "primer");

		assertEquals("primer", configValues.get("clau.a"));
		bdRetorna("clau.a", "segon");
		for (int i = 0; i < 100; i++) {
			assertEquals("primer", configValues.get("clau.a"));
			configValues.getProperties();
		}
		verify(dataSource, times(1)).getConnection();
	}

	@Test
	public void cache_quanCaducaEsTornaALlegir() throws Exception {
		bdRetorna("clau.a", "primer");
		assertEquals("primer", configValues.get("clau.a"));

		bdRetorna("clau.a", "segon");
		assertEquals("segon", configValues.get("clau.a"));
		verify(dataSource, times(2)).getConnection();
	}

	@Test
	public void cache_siLaBdFallaNoEsReintentaACadaLectura() throws Exception {
		// Amb el pool exhaurit cada intent espera el blocking-timeout: no s'ha de repetir a cada lectura
		configValues.setDuradaCacheMs(ConfigValues.DURADA_CACHE_MS);
		when(dataSource.getConnection()).thenThrow(new SQLException("IJ000453"));

		for (int i = 0; i < 100; i++) {
			assertEquals("defecte", configValues.get("clau.a", "defecte"));
		}
		verify(dataSource, times(1)).getConnection();
	}

	@Test
	public void cache_siLaBdFallaEnRecarregarConservaElsValorsAnteriors() throws Exception {
		bdRetorna("clau.a", "valor-bd");
		assertEquals("valor-bd", configValues.get("clau.a"));

		when(dataSource.getConnection()).thenThrow(new SQLException("IJ000453"));

		assertEquals("valor-bd", configValues.get("clau.a", "defecte"));
	}

	@Test
	public void log_lesCredencialsSEmmascaren() {
		assertEquals(ConfigValues.VALOR_EMMASCARAT, ConfigValues.valorPerAlLog("CREDENTIALS", "secret"));
		assertEquals("5", ConfigValues.valorPerAlLog("INT", "5"));
		assertEquals("S", ConfigValues.valorPerAlLog(null, "S"));
		assertTrue(ConfigValues.SQL_TOTES.contains("TYPE_CODE"));
	}

	@Test
	public void cache_dura5MinutsIEsReintentaAlMinutSiFalla() {
		assertEquals(300_000L, ConfigValues.DURADA_CACHE_MS);
		assertEquals(60_000L, ConfigValues.DURADA_REINTENT_MS);
	}

	@Test
	public void invalida_laProximaLecturaTornaALlegirLaBd() throws Exception {
		configValues.setDuradaCacheMs(ConfigValues.DURADA_CACHE_MS);
		bdRetorna("clau.a", "primer");
		assertEquals("primer", configValues.get("clau.a"));

		bdRetorna("clau.a", "segon");
		assertEquals("Sense invalidar es conserva el valor en memòria", "primer", configValues.get("clau.a"));

		ConfigValues.invalida();
		assertEquals("segon", configValues.get("clau.a"));
		assertEquals("segon", configValues.get("clau.a"));
		verify(dataSource, times(2)).getConnection();
	}

	@Test
	public void invalida_afectaTotesLesInstancies() throws Exception {
		configValues.setDuradaCacheMs(ConfigValues.DURADA_CACHE_MS);
		ConfigValues altra = new ConfigValues();
		altra.setDataSource(dataSource);
		altra.setDuradaCacheMs(ConfigValues.DURADA_CACHE_MS);
		bdRetorna("clau.a", "primer");
		assertEquals("primer", configValues.get("clau.a"));
		assertEquals("primer", altra.get("clau.a"));

		bdRetorna("clau.a", "segon");
		ConfigValues.invalida();

		assertEquals("segon", configValues.get("clau.a"));
		assertEquals("segon", altra.get("clau.a"));
	}

	@Test
	public void invalida_siLaRecarregaFallaConservaElsValorsAnteriors() throws Exception {
		configValues.setDuradaCacheMs(ConfigValues.DURADA_CACHE_MS);
		bdRetorna("clau.a", "valor-bd");
		assertEquals("valor-bd", configValues.get("clau.a"));

		when(dataSource.getConnection()).thenThrow(new SQLException("IJ000453"));
		ConfigValues.invalida();

		assertEquals("valor-bd", configValues.get("clau.a", "defecte"));
	}

	@Test
	public void cache_mentreUnFilRecarregaElsAltresNoEsperen() throws Exception {
		bdRetorna("clau.a", "valor-bd");
		assertEquals("valor-bd", configValues.get("clau.a"));

		// La recàrrega es queda bloquejada (com si esperés el pool) fins que la deixem continuar
		CountDownLatch dinsCarrega = new CountDownLatch(1);
		CountDownLatch continua = new CountDownLatch(1);
		when(dataSource.getConnection()).thenAnswer(inv -> {
			dinsCarrega.countDown();
			continua.await(10, TimeUnit.SECONDS);
			return connection;
		});
		executor = Executors.newFixedThreadPool(2);
		Future<String> recarrega = executor.submit(() -> configValues.get("clau.a"));
		assertTrue("La recàrrega no ha començat", dinsCarrega.await(5, TimeUnit.SECONDS));

		Future<String> altre = executor.submit(() -> configValues.get("clau.a"));
		assertEquals("Un altre fil ha d'obtenir el valor anterior sense esperar la recàrrega",
				"valor-bd", altre.get(2, TimeUnit.SECONDS));

		continua.countDown();
		assertEquals("valor-bd", recarrega.get(5, TimeUnit.SECONDS));
	}

}
