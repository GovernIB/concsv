package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaResource;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaRepository;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves de {@link ExecucioMassivaCuaHelper}.
 */
public class ExecucioMassivaCuaHelperTest {

	private ExecucioMassivaRepository execucioRepository;
	private ExecucioMassivaContingutRepository contingutRepository;
	private ExecucioMassivaCuaHelper helper;

	@Before
	public void setUp() {
		execucioRepository = mock(ExecucioMassivaRepository.class);
		contingutRepository = mock(ExecucioMassivaContingutRepository.class);
		helper = new ExecucioMassivaCuaHelper(execucioRepository, contingutRepository);
	}

	private static ExecucioMassivaEntity execucio(Long id, String creador, boolean correu) {
		ExecucioMassivaEntity execucio = new ExecucioMassivaEntity();
		execucio.setId(id);
		execucio.setTipus(ExecucioMassivaTipusEnum.ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS);
		execucio.setEstat(ExecucioMassivaEstatEnum.PENDENT);
		execucio.setCreatedBy(creador);
		execucio.setEnviarCorreu(correu);
		return execucio;
	}

	private void reclamarRetorna(Long id, int resultat) {
		when(execucioRepository.reclamar(eq(id), any(), any(), any(), any(), any())).thenReturn(resultat);
	}

	// --- Reclamació --------------------------------------------------------------------------

	@Test
	public void reclamaLaPrimeraCandidataQueEsPotAgafar() {
		when(execucioRepository.findIdsProcessables(any(), any(), any(), any(), any(), any(Pageable.class)))
				.thenReturn(Arrays.asList(1L, 2L, 3L));
		// Un altre node s'ha endut l'1; la 2 és nostra
		reclamarRetorna(1L, 0);
		reclamarRetorna(2L, 1);

		assertEquals(Long.valueOf(2L), helper.reclamarPerProcessar());

		verify(execucioRepository, never()).reclamar(eq(3L), any(), any(), any(), any(), any());
	}

	@Test
	public void sensePendentsNoReclamaRes() {
		when(execucioRepository.findIdsProcessables(any(), any(), any(), any(), any(), any(Pageable.class)))
				.thenReturn(Collections.emptyList());

		assertNull(helper.reclamarPerProcessar());

		verify(execucioRepository, never()).reclamar(anyLong(), any(), any(), any(), any(), any());
	}

	@Test
	public void siTotesLesCandidatesLesHaAgafatAltresNodesNoRetornaRes() {
		when(execucioRepository.findIdsProcessables(any(), any(), any(), any(), any(), any(Pageable.class)))
				.thenReturn(Arrays.asList(1L, 2L));
		reclamarRetorna(1L, 0);
		reclamarRetorna(2L, 0);

		assertNull(helper.reclamarPerProcessar());
	}

	@Test
	public void elNodeNoEstaBuit() {
		assertFalse(ExecucioMassivaCuaHelper.NODE.trim().isEmpty());
		assertTrue(ExecucioMassivaCuaHelper.NODE.length() <= 255);
	}

	// --- Cancel·lació ------------------------------------------------------------------------

	@Test
	public void cancelaAAlInstantUnaExecucioQueCapNodeNoHaAgafat() {
		when(execucioRepository.findById(5L)).thenReturn(Optional.of(execucio(5L, "u1", false)));
		when(execucioRepository.cancelarPendent(eq(5L), any(), eq("u1"), any(), any())).thenReturn(1);

		assertEquals(ExecucioMassivaResource.RESULTAT_CANCELADA, helper.cancelar(5L, "u1"));

		verify(contingutRepository).cancelarPendents(
				eq(5L), any(), eq(ExecucioMassivaContingutEstatEnum.PENDENT), eq(ExecucioMassivaContingutEstatEnum.CANCELAT));
		verify(execucioRepository, never()).solicitarCancelacio(anyLong(), any(), any());
	}

	@Test
	public void unaExecucioEnCursNomesDemanaLaCancelacio() {
		when(execucioRepository.findById(5L)).thenReturn(Optional.of(execucio(5L, "u1", false)));
		when(execucioRepository.cancelarPendent(eq(5L), any(), any(), any(), any())).thenReturn(0);
		when(execucioRepository.solicitarCancelacio(eq(5L), any(), eq("u1"))).thenReturn(1);

		assertEquals(ExecucioMassivaResource.RESULTAT_EN_CURS, helper.cancelar(5L, "u1"));

		verify(contingutRepository, never()).cancelarPendents(anyLong(), any(), any(), any());
	}

	@Test
	public void demanarLaCancelacioDuesVegadesNoEsUnError() {
		ExecucioMassivaEntity enCurs = execucio(5L, "u1", false);
		when(execucioRepository.findById(5L)).thenReturn(Optional.of(enCurs));
		when(execucioRepository.cancelarPendent(eq(5L), any(), any(), any(), any())).thenReturn(0);
		// Ja demanada: l'actualització no canvia cap fila però l'execució no ha acabat
		when(execucioRepository.solicitarCancelacio(eq(5L), any(), any())).thenReturn(0);

		assertEquals(ExecucioMassivaResource.RESULTAT_EN_CURS, helper.cancelar(5L, "u1"));
	}

	@Test
	public void cancelarUnaExecucioQueHaAcabatJustAraEsUnError() {
		ExecucioMassivaEntity acabada = execucio(5L, "u1", false);
		when(execucioRepository.findById(5L)).thenReturn(Optional.of(acabada));
		when(execucioRepository.cancelarPendent(eq(5L), any(), any(), any(), any())).thenReturn(0);
		when(execucioRepository.solicitarCancelacio(eq(5L), any(), any())).thenReturn(0);
		// Després de les consultes, l'execució ja té data de fi
		ExecucioMassivaEntity ambDataFi = execucio(5L, "u1", false);
		ambDataFi.setDataFi(LocalDateTime.now());
		when(execucioRepository.findById(5L)).thenReturn(Optional.of(acabada), Optional.of(ambDataFi));

		try {
			helper.cancelar(5L, "u1");
			fail("S'esperava IllegalStateException");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("ja ha acabat"));
		}
	}

	@Test
	public void noEsPotCancelarUnaExecucioJaAcabada() {
		ExecucioMassivaEntity acabada = execucio(5L, "u1", false);
		acabada.setDataFi(LocalDateTime.now());
		when(execucioRepository.findById(5L)).thenReturn(Optional.of(acabada));

		try {
			helper.cancelar(5L, "u1");
			fail("S'esperava IllegalStateException");
		} catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("ja ha acabat"));
		}
		verify(execucioRepository, never()).cancelarPendent(anyLong(), any(), any(), any(), any());
	}

	@Test(expected = IllegalStateException.class)
	public void cancelarUnaExecucioInexistentFalla() {
		when(execucioRepository.findById(9L)).thenReturn(Optional.empty());

		helper.cancelar(9L, "u1");
	}

	@Test
	public void laCancelacioDemanadaEsLlegeixDeLaBaseDeDades() {
		when(execucioRepository.getCancelacioData(5L)).thenReturn(null, LocalDateTime.now());

		assertFalse(helper.isCancelacioSolicitada(5L));
		assertTrue(helper.isCancelacioSolicitada(5L));
	}

}
