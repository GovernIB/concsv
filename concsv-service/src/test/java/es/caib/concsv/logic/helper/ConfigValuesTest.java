package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests de {@link ConfigValues}: lectura del valor de la base de dades, caiguda al valor per defecte
 * si la base de dades falla o no el té, i conversió de tipus. Sense MicroProfile Config a les
 * proves, la propietat "del fitxer" no hi és mai.
 */
public class ConfigValuesTest {

	private ConfigValues configValues;
	private DataSource dataSource;
	private PreparedStatement statement;
	private ResultSet resultSet;

	@Before
	public void setUp() throws Exception {
		dataSource = mock(DataSource.class);
		Connection connection = mock(Connection.class);
		statement = mock(PreparedStatement.class);
		resultSet = mock(ResultSet.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		configValues = new ConfigValues();
		configValues.setDataSource(dataSource);
	}

	/** La consulta troba una fila amb aquest valor. */
	private void bdRetorna(String valor) throws SQLException {
		when(resultSet.next()).thenReturn(true);
		when(resultSet.getString(1)).thenReturn(valor);
	}

	@Test
	public void get_prenElValorDeLaBd() throws Exception {
		bdRetorna("valor-bd");

		assertEquals("valor-bd", configValues.get("clau.a"));
		verify(statement).setString(1, "clau.a");
	}

	@Test
	public void get_consultaNomesLesPropietatsEditables() {
		assertTrue(ConfigValues.SQL.contains("JBOSS_PROPERTY = 0"));
	}

	@Test
	public void get_senseFilaOAmbValorNulRetornaElPerDefecte() throws Exception {
		when(resultSet.next()).thenReturn(false);
		assertNull(configValues.get("clau.a"));
		assertEquals("defecte", configValues.get("clau.a", "defecte"));

		bdRetorna(null);
		assertEquals("defecte", configValues.get("clau.a", "defecte"));
	}

	@Test
	public void get_siLaBdFallaRetornaElPerDefecteSenseLlancarExcepcio() throws Exception {
		when(dataSource.getConnection()).thenThrow(new SQLException("ORA-00942"));

		assertEquals("defecte", configValues.get("clau.a", "defecte"));
		assertNull(configValues.get("clau.a"));
	}

	@Test
	public void getProperties_incloLesPropietatsEditablesAmbValorDeLaBd() throws Exception {
		when(resultSet.next()).thenReturn(true, true, false);
		when(resultSet.getString(1)).thenReturn("clau.a", "clau.b");
		when(resultSet.getString(2)).thenReturn("valor-a", "");

		java.util.Properties properties = configValues.getProperties();

		assertEquals("valor-a", properties.getProperty("clau.a"));
		// Un valor buit no substitueix el del fitxer
		assertNull(properties.getProperty("clau.b"));
		assertTrue(ConfigValues.SQL_TOTES.contains("JBOSS_PROPERTY = 0"));
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
	public void getBoolean_ambSiNoITrueFalse() throws Exception {
		bdRetorna("S");
		assertTrue(configValues.getBoolean("clau.a", false));
		bdRetorna("N");
		assertFalse(configValues.getBoolean("clau.a", true));
		bdRetorna("true");
		assertTrue(configValues.getBoolean("clau.a", false));
		bdRetorna("false");
		assertFalse(configValues.getBoolean("clau.a", true));
	}

	@Test
	public void getBoolean_senseValorRetornaElPerDefecte() throws Exception {
		when(resultSet.next()).thenReturn(false);
		assertTrue(configValues.getBoolean("clau.a", true));
		assertFalse(configValues.getBoolean("clau.a", false));
	}

	@Test
	public void getInt_iGetLong_enterValid() throws Exception {
		bdRetorna(" 45 ");
		assertEquals(45, configValues.getInt("clau.a", 1));
		assertEquals(45L, configValues.getLong("clau.a", 1L));
	}

	@Test
	public void getInt_iGetLong_enterInvalidORUnaBdBuidaRetornenElPerDefecte() throws Exception {
		bdRetorna("abc");
		assertEquals(30, configValues.getInt("clau.a", 30));
		assertEquals(30L, configValues.getLong("clau.a", 30L));

		when(resultSet.next()).thenReturn(false);
		assertEquals(365, configValues.getInt("clau.a", 365));
	}

	@Test
	public void getInt_siNoCapAInteger_retornaElPerDefecte() throws Exception {
		bdRetorna("99999999999");
		assertEquals(7, configValues.getInt("clau.a", 7));
		assertEquals(99999999999L, configValues.getLong("clau.a", 1L));
	}

}
