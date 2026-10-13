package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import es.caib.comanda.ms.salut.helper.IntegracioApp;
import es.caib.concsv.logic.helper.MonitorIntegracioHelper.Registre;
import es.caib.concsv.logic.intf.exception.DocumentNotExistException;
import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.logic.intf.model.IntegracioAccioTipusEnum;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;

/**
 * Tests del pas de les peticions a les integracions cap al monitor d'integracions del backoffice.
 */
public class IntegracionsHelperMonitorTest {

	private IntegracionsHelper helper;
	private MonitorIntegracioHelper monitor;

	@Before
	public void setUp() {
		helper = new IntegracionsHelper();
		helper.init();
		monitor = mock(MonitorIntegracioHelper.class);
		helper.setMonitorIntegracioHelper(monitor);
	}

	private Registre registreEncuat() {
		ArgumentCaptor<Registre> captor = ArgumentCaptor.forClass(Registre.class);
		verify(monitor).encua(captor.capture());
		return captor.getValue();
	}

	@Test
	public void senseExcepcioEsRegistraComCorrecta() {
		Map<String, String> params = IntegracionsHelper.params("uuid", "abc");

		helper.addOperation(IntegracioApp.ARX, System.currentTimeMillis() - 5, "Consulta", "http://arxiu", params, null);

		Registre registre = registreEncuat();
		assertEquals(IntegracioCodiEnum.ARXIU, registre.getCodi());
		assertEquals(IntegracioAccioEstatEnum.OK, registre.getEstat());
		assertEquals(IntegracioAccioTipusEnum.ENVIAMENT, registre.getTipus());
		assertEquals("Consulta", registre.getDescripcio());
		assertEquals("http://arxiu", registre.getEndpoint());
		assertEquals(params, registre.getParametres());
		assertTrue(registre.getTempsResposta() >= 5);
		assertNull(registre.getExcepcioMessage());
		assertNull(registre.getExcepcioStacktrace());
		assertNull(registre.getErrorDescripcio());
	}

	@Test
	public void ambExcepcioEsRegistraComErrorAmbLaTraca() {
		helper.addOperation(IntegracioApp.CUS, System.currentTimeMillis(), "Consulta de custodia", null,
				IntegracionsHelper.params("hash", "h1"), new DocumentNotExistException("hash:h1"));

		Registre registre = registreEncuat();
		assertEquals(IntegracioCodiEnum.CUSTODIA, registre.getCodi());
		assertEquals(IntegracioAccioEstatEnum.ERROR, registre.getEstat());
		assertEquals("Error en: Consulta de custodia", registre.getErrorDescripcio());
		assertTrue(registre.getExcepcioMessage().startsWith("DocumentNotExistException"));
		assertNotNull(registre.getExcepcioStacktrace());
		assertTrue(registre.getExcepcioStacktrace().contains("DocumentNotExistException"));
	}

	@Test
	public void cadaIntegracioTeElSeuCodiDelMonitor() {
		assertEquals(IntegracioCodiEnum.ARXIU, IntegracionsHelper.codiMonitor(IntegracioApp.ARX));
		assertEquals(IntegracioCodiEnum.CUSTODIA, IntegracionsHelper.codiMonitor(IntegracioApp.CUS));
		assertEquals(IntegracioCodiEnum.VALIDACIO_FIRMES, IntegracionsHelper.codiMonitor(IntegracioApp.VFI));
		assertEquals(IntegracioCodiEnum.CONVERSIO, IntegracionsHelper.codiMonitor(IntegracioApp.CDO));
		assertNull(IntegracionsHelper.codiMonitor(IntegracioApp.DIS));
	}

	@Test
	public void unaIntegracioQueNoEsMostraAlMonitorNoEsDesa() {
		helper.addOperation(IntegracioApp.DIS, System.currentTimeMillis(), "x", null, null, null);

		verify(monitor, never()).encua(any());
	}

	@Test
	public void unaFalladaDelMonitorNoArribaAQuiHaFetLaCrida() {
		doThrow(new IllegalStateException("cua trencada")).when(monitor).encua(any());

		helper.addOperation(IntegracioApp.VFI, System.currentTimeMillis(), "Validacio", null, null, null);
	}

	@Test
	public void senseMonitorInjectatSegueixFuncionant() {
		IntegracionsHelper senseMonitor = new IntegracionsHelper();
		senseMonitor.init();

		senseMonitor.addOperation(IntegracioApp.ARX, System.currentTimeMillis(), "x", null, null, null);
		senseMonitor.addOperation(IntegracioApp.ARX, System.currentTimeMillis(), "x", null, null, new RuntimeException());
	}

	@Test
	public void elsParametresNullsEsDescarten() {
		Map<String, String> params = IntegracionsHelper.params("uuid", "u1", "csv", null, null, "x");

		assertEquals(1, params.size());
		assertEquals("u1", params.get("uuid"));
	}

}
