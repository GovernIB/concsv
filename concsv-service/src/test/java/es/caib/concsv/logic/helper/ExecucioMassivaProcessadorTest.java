package es.caib.concsv.logic.helper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves de {@link ExecucioMassivaProcessador}.
 */
public class ExecucioMassivaProcessadorTest {

	private ExecucioMassivaCuaHelper cuaHelper;
	private ExecucioMassivaHelper helper;
	private ExecucioMassivaProcessador processador;

	@Before
	public void setUp() {
		cuaHelper = mock(ExecucioMassivaCuaHelper.class);
		helper = mock(ExecucioMassivaHelper.class);
		processador = new ExecucioMassivaProcessador(cuaHelper, helper);
		processador.init();
	}

	@After
	public void tearDown() {
		processador.destrueix();
	}

	@Test
	public void sensePendentsNoFaRes() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(null);

		processador.executeNextMassiveScheduledTask();

		verify(helper, never()).idsPendents(anyLong());
		verify(helper, never()).finalitza(anyLong(), anyString());
	}

	@Test
	public void processaElsElementsPendentsEnOrdreIAcabaTancantLExecucioIAvisant() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.idsPendents(1L)).thenReturn(Arrays.asList(10L, 11L, 12L));

		processador.executeNextMassiveScheduledTask();

		InOrder ordre = inOrder(helper);
		ordre.verify(helper).executaContingut(10L);
		ordre.verify(helper).executaContingut(11L);
		ordre.verify(helper).executaContingut(12L);
		ordre.verify(helper).finalitza(1L, null);
	}

	@Test
	public void unaCancelacioDemanadaAturaElProcesAbansDelSeguentElement() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.idsPendents(1L)).thenReturn(Arrays.asList(10L, 11L, 12L));
		// Es demana cancel·lar mentre es tracta el primer
		when(cuaHelper.isCancelacioSolicitada(1L)).thenReturn(false, true);

		processador.executeNextMassiveScheduledTask();

		verify(helper).executaContingut(10L);
		verify(helper, never()).executaContingut(11L);
		verify(helper, never()).executaContingut(12L);
		// Es tanca igualment: el tancament és el que marca els pendents com a cancel·lats
		verify(helper).finalitza(1L, null);
	}

	@Test
	public void unElementQueFallaAlDesarEsMarcaComAErrorIElsAltresContinuen() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.idsPendents(1L)).thenReturn(Arrays.asList(10L, 11L));
		doThrow(new IllegalStateException("Rollback")).when(helper).executaContingut(10L);

		processador.executeNextMassiveScheduledTask();

		verify(helper).marcaContingutError(10L, "Rollback");
		verify(helper).executaContingut(11L);
		verify(helper).finalitza(1L, null);
	}

	@Test
	public void unaFalladaDelProcesTancaLExecucioAmbElMotiu() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.idsPendents(1L)).thenThrow(new IllegalStateException("BD caiguda"));

		processador.executeNextMassiveScheduledTask();

		verify(helper).finalitza(1L, "BD caiguda");
	}

	@Test
	public void siNoEsPotTancarLExecucioNoPropagaLExcepcio() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.idsPendents(1L)).thenReturn(Collections.emptyList());
		doThrow(new IllegalStateException("BD caiguda")).when(helper).finalitza(1L, null);

		// No ha de propagar l'excepció: el planificador deixaria la tasca per sempre
		processador.executeNextMassiveScheduledTask();

	}

	@Test
	public void elsElementsEsTractenAmbLUsuariCreadorIEsRestauraLAutenticacio() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.creador(1L)).thenReturn("creador");
		when(helper.idsPendents(1L)).thenReturn(Arrays.asList(10L, 11L));
		List<String> usuaris = new ArrayList<>();
		doAnswer(invocation -> {
			usuaris.add(SecurityContextHolder.getContext().getAuthentication().getName());
			return null;
		}).when(helper).executaContingut(anyLong());
		doAnswer(invocation -> {
			// El tancament s'executa un cop restaurat l'usuari de sistema del planificador
			usuaris.add(SecurityContextHolder.getContext().getAuthentication().getName());
			return null;
		}).when(helper).finalitza(anyLong(), isNull());
		SecurityContextHolder.getContext().setAuthentication(ExecucioMassivaHelper.autenticacio("concsv"));

		processador.executeNextMassiveScheduledTask();

		// L'auditoria s'omple en el commit de cada element, així que l'usuari és el mateix per a tots
		assertEquals(Arrays.asList("creador", "creador", "concsv"), usuaris);
		assertEquals("concsv", SecurityContextHolder.getContext().getAuthentication().getName());
		SecurityContextHolder.clearContext();
	}

	@Test
	public void siElProcesFallaTambeEsRestauraLAutenticacio() {
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.creador(1L)).thenReturn("creador");
		when(helper.idsPendents(1L)).thenThrow(new IllegalStateException("BD caiguda"));
		SecurityContextHolder.getContext().setAuthentication(ExecucioMassivaHelper.autenticacio("concsv"));

		processador.executeNextMassiveScheduledTask();

		assertEquals("concsv", SecurityContextHolder.getContext().getAuthentication().getName());
		SecurityContextHolder.clearContext();
	}

	@Test
	public void senseAutenticacioPrevisElContextQuedaBuit() {
		SecurityContextHolder.clearContext();
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L);
		when(helper.creador(1L)).thenReturn("creador");
		when(helper.idsPendents(1L)).thenReturn(Collections.emptyList());

		processador.executeNextMassiveScheduledTask();

		assertNull(SecurityContextHolder.getContext().getAuthentication());
	}

	@Test
	public void nomesUnaExecucioAlhoraPerNode() throws Exception {
		CountDownLatch iniciat = new CountDownLatch(1);
		CountDownLatch alliberat = new CountDownLatch(1);
		when(cuaHelper.reclamarPerProcessar()).thenReturn(1L).thenReturn(2L);
		when(helper.idsPendents(1L)).thenAnswer(invocation -> {
			iniciat.countDown();
			// L'execució 1 queda en procés fins que el test ho permeti
			assertTrue(alliberat.await(5, TimeUnit.SECONDS));
			return Collections.emptyList();
		});
		Thread primera = new Thread(processador::executeNextMassiveScheduledTask);
		primera.start();
		assertTrue(iniciat.await(5, TimeUnit.SECONDS));

		// Una segona comprovació mentre la primera és en curs no reclama res
		processador.executeNextMassiveScheduledTask();

		verify(cuaHelper, org.mockito.Mockito.times(1)).reclamarPerProcessar();
		alliberat.countDown();
		primera.join(5000);
		// I un cop acabada, la cua torna a funcionar
		processador.executeNextMassiveScheduledTask();
		verify(cuaHelper, org.mockito.Mockito.times(2)).reclamarPerProcessar();
		verify(helper).finalitza(org.mockito.ArgumentMatchers.eq(1L), isNull());
	}

}
