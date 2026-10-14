package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource.DiagnosticResult;

/**
 * Tests del diagnòstic ràpid de les integracions: el resultat, mai l'excepció, és el que arriba al
 * backoffice.
 */
public class IntegracioDiagnosticHelperTest {

	private ConfigHelper configHelper;
	private IntegracioDiagnosticHelper helper;

	@Before
	public void setUp() {
		configHelper = mock(ConfigHelper.class);
		helper = spy(new IntegracioDiagnosticHelper(configHelper));
	}

	private void prova(IntegracioCodiEnum codi, Callable<String> callable) {
		doReturn(callable).when(helper).prova(codi);
	}

	@Test
	public void diagnosticaLesQuatreIntegracions() {
		List<String> codis = helper.codis();

		assertEquals(List.of("ARXIU", "CUSTODIA", "VALIDACIO_FIRMES", "CONVERSIO"), codis);
	}

	@Test
	public void unaProvaCorrectaRetornaElTextIElTemps() {
		prova(IntegracioCodiEnum.ARXIU, () -> "L'Arxiu ha respost correctament");

		DiagnosticResult resultat = helper.executa(IntegracioCodiEnum.ARXIU);

		assertTrue(resultat.isCorrecte());
		assertTrue(resultat.getProva().startsWith("L'Arxiu ha respost correctament ("));
		assertTrue(resultat.getProva().endsWith(" ms)"));
		assertNull(resultat.getErrMsg());
	}

	@Test
	public void unaProvaQueFallaNoLlancaExcepcio() {
		prova(IntegracioCodiEnum.CUSTODIA, () -> {
			throw new IllegalStateException("connexió refusada");
		});

		DiagnosticResult resultat = helper.executa(IntegracioCodiEnum.CUSTODIA);

		assertFalse(resultat.isCorrecte());
		assertNull(resultat.getProva());
		assertEquals("IllegalStateException: connexió refusada", resultat.getErrMsg());
	}

	@Test
	public void laContrasenyaDeLArxiuNoEsMostraMai() {
		when(configHelper.getEnvironmentProperty(eq(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_PASSWORD), any()))
				.thenReturn("secret123");
		prova(IntegracioCodiEnum.ARXIU, () -> {
			throw new RuntimeException("Error cridant http://arxiu?password=secret123");
		});

		DiagnosticResult resultat = helper.executa(IntegracioCodiEnum.ARXIU);

		assertFalse(resultat.getErrMsg().contains("secret123"));
		assertTrue(resultat.getErrMsg().contains("***"));
	}

	@Test
	public void unServeiQueNoResponEsCortaPelTemps() {
		helper.timeoutMs = 200;
		prova(IntegracioCodiEnum.VALIDACIO_FIRMES, () -> {
			Thread.sleep(5000);
			return "mai";
		});

		long t0 = System.currentTimeMillis();
		DiagnosticResult resultat = helper.executa(IntegracioCodiEnum.VALIDACIO_FIRMES);

		assertFalse(resultat.isCorrecte());
		assertTrue(resultat.getErrMsg().startsWith("El servei no ha respost"));
		assertTrue("No ha d'esperar a la prova", System.currentTimeMillis() - t0 < 3000);
	}

	@Test
	public void unaPropietatSenseDefinirEsDiuAmbElSeuNom() {
		// El mock retorna null per a totes les propietats.
		DiagnosticResult resultat = helper.executa(IntegracioCodiEnum.ARXIU);

		assertFalse(resultat.isCorrecte());
		assertTrue(resultat.getErrMsg().contains(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_ENDPOINT));
	}

}
