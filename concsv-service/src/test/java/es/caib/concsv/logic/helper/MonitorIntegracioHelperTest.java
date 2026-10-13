package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Reader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import es.caib.concsv.logic.helper.MonitorIntegracioHelper.Registre;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.logic.intf.model.IntegracioAccioTipusEnum;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;

/**
 * Tests de l'escriptor del monitor d'integracions: cua acotada, lots, truncat, interruptor, esborrat
 * i, sobretot, que cap error de base de dades surti de la classe.
 */
public class MonitorIntegracioHelperTest {

	private MonitorIntegracioHelper helper;
	private ConfigValues configValues;
	private DataSource dataSource;
	private Connection connection;
	private PreparedStatement statement;

	@Before
	public void setUp() throws Exception {
		helper = new MonitorIntegracioHelper();
		configValues = mock(ConfigValues.class);
		when(configValues.getBoolean(PropertyConfig.PROP_INTEGRACIO_MONITOR_ACTIU, true)).thenReturn(true);
		dataSource = mock(DataSource.class);
		connection = mock(Connection.class);
		statement = mock(PreparedStatement.class);
		when(dataSource.getConnection()).thenReturn(connection);
		when(connection.prepareStatement(anyString())).thenReturn(statement);
		helper.setConfigValues(configValues);
		helper.setDataSource(dataSource);
	}

	private Registre registre(IntegracioAccioEstatEnum estat) {
		return Registre.builder().
				data(new Date()).
				codi(IntegracioCodiEnum.ARXIU).
				descripcio("Consulta de document per UUID").
				endpoint("http://arxiu").
				parametres(Map.of("uuid", "u1")).
				tipus(IntegracioAccioTipusEnum.ENVIAMENT).
				tempsResposta(12).
				estat(estat).
				build();
	}

	@Test
	public void desaLesPeticionsPendentsEnUnSolLot() throws Exception {
		helper.encua(registre(IntegracioAccioEstatEnum.OK));
		helper.encua(registre(IntegracioAccioEstatEnum.ERROR));

		int desades = helper.flush();

		assertEquals(2, desades);
		verify(connection).prepareStatement(MonitorIntegracioHelper.SQL_INSERT);
		verify(statement, times(2)).addBatch();
		verify(statement).executeBatch();
		verify(statement, times(2)).setString(2, "ARXIU");
		verify(statement).setString(8, "OK");
		verify(statement).setString(8, "ERROR");
		verify(statement, times(2)).setString(12, MonitorIntegracioHelper.USUARI);
		assertEquals(0, helper.pendents());
	}

	@Test
	public void elsParametresEsDesenComAJson() throws Exception {
		helper.encua(registre(IntegracioAccioEstatEnum.OK));

		helper.flush();

		ArgumentCaptor<Reader> clob = ArgumentCaptor.forClass(Reader.class);
		verify(statement).setCharacterStream(eq(5), clob.capture(), anyInt());
		char[] buffer = new char[200];
		int llegits = clob.getValue().read(buffer);
		assertEquals("{\"uuid\":\"u1\"}", new String(buffer, 0, llegits));
	}

	@Test
	public void senseParametresNiTracaElsClobsVanANull() throws Exception {
		helper.encua(Registre.builder().
				data(new Date()).
				codi(IntegracioCodiEnum.VALIDACIO_FIRMES).
				estat(IntegracioAccioEstatEnum.OK).
				build());

		helper.flush();

		verify(statement).setNull(5, Types.CLOB);
		verify(statement).setNull(11, Types.CLOB);
	}

	@Test
	public void elsTextosLlargsEsTrunquenALaMidaDeLaColumna() throws Exception {
		String llarg = "x".repeat(MonitorIntegracioHelper.MIDA_TRACA * 2);
		helper.encua(Registre.builder().
				data(new Date()).
				codi(IntegracioCodiEnum.CUSTODIA).
				descripcio(llarg).
				endpoint(llarg).
				estat(IntegracioAccioEstatEnum.ERROR).
				errorDescripcio(llarg).
				excepcioMessage(llarg).
				excepcioStacktrace(llarg).
				build());

		helper.flush();

		ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
		verify(statement).setString(eq(3), text.capture());
		assertEquals(MonitorIntegracioHelper.MIDA_DESCRIPCIO, text.getValue().length());
		ArgumentCaptor<Integer> midaTraca = ArgumentCaptor.forClass(Integer.class);
		verify(statement).setCharacterStream(eq(11), any(Reader.class), midaTraca.capture());
		assertEquals(MonitorIntegracioHelper.MIDA_TRACA, midaTraca.getValue().intValue());
	}

	@Test
	public void laCuaEsAcotadaINoBloqueja() {
		for (int i = 0; i < MonitorIntegracioHelper.CAPACITAT_CUA + 10; i++) {
			helper.encua(registre(IntegracioAccioEstatEnum.OK));
		}

		assertEquals(MonitorIntegracioHelper.CAPACITAT_CUA, helper.pendents());
	}

	@Test
	public void ambElMonitorDesactivatNoEsDesaRes() throws Exception {
		when(configValues.getBoolean(PropertyConfig.PROP_INTEGRACIO_MONITOR_ACTIU, true)).thenReturn(false);
		helper.encua(registre(IntegracioAccioEstatEnum.OK));

		int desades = helper.flush();

		assertEquals(0, desades);
		assertEquals(0, helper.pendents());
		verify(dataSource, never()).getConnection();
		// Un cop desactivat, ja no s'encua res més.
		helper.encua(registre(IntegracioAccioEstatEnum.OK));
		assertEquals(0, helper.pendents());
	}

	@Test
	public void unErrorDeBaseDeDadesNoArribaAQuiFaFlush() throws Exception {
		when(dataSource.getConnection()).thenThrow(new SQLException("ORA-00942: taula inexistent"));
		helper.encua(registre(IntegracioAccioEstatEnum.OK));

		int desades = helper.flush();

		assertEquals(0, desades);
		// El lot es descarta: no s'acumula sense fi si la base de dades no respon.
		assertEquals(0, helper.pendents());
	}

	@Test
	public void unErrorEnLlegirLaPropietatConservaElDarrerValor() throws Exception {
		when(configValues.getBoolean(PropertyConfig.PROP_INTEGRACIO_MONITOR_ACTIU, true))
				.thenThrow(new IllegalStateException("sense base de dades"));
		helper.encua(registre(IntegracioAccioEstatEnum.OK));

		assertEquals(1, helper.flush());
	}

	@Test
	public void esborraPerLotsFinsQueNoQuedenAntics() throws Exception {
		when(statement.executeUpdate()).thenReturn(MonitorIntegracioHelper.LOT_ESBORRAT, MonitorIntegracioHelper.LOT_ESBORRAT, 37);

		int esborrades = helper.esborraAntics(30);

		assertEquals(2 * MonitorIntegracioHelper.LOT_ESBORRAT + 37, esborrades);
		verify(statement, times(3)).executeUpdate();
		ArgumentCaptor<Timestamp> limit = ArgumentCaptor.forClass(Timestamp.class);
		verify(statement, times(3)).setTimestamp(eq(1), limit.capture());
		long trentaDies = 30L * 24 * 60 * 60 * 1000;
		long diferencia = System.currentTimeMillis() - limit.getValue().getTime();
		assertTrue("El límit ha de ser de fa uns 30 dies", Math.abs(diferencia - trentaDies) < 2 * 60 * 60 * 1000);
	}

	@Test
	public void unNombreDeDiesNoPositiuNoEsborraMai() throws Exception {
		assertEquals(0, helper.esborraAntics(0));
		assertEquals(0, helper.esborraAntics(-5));

		verify(dataSource, never()).getConnection();
		verify(statement, never()).executeUpdate();
		verify(statement, never()).setTimestamp(anyInt(), any(Timestamp.class));
		verify(statement, never()).setLong(anyInt(), anyLong());
	}

}
