package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.model.DocumentExclosEstatEnum;
import es.caib.concsv.logic.intf.model.ElementTipusEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.logic.resourceservice.DocumentExclosResourceServiceImpl;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaContingutEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves de {@link ExecucioMassivaHelper}.
 */
public class ExecucioMassivaHelperTest {

	private ExecucioMassivaRepository execucioRepository;
	private ExecucioMassivaContingutRepository contingutRepository;
	private DocumentExclosResourceServiceImpl documentService;
	private ExecucioMassivaHelper helper;

	@Before
	public void setUp() {
		execucioRepository = mock(ExecucioMassivaRepository.class);
		contingutRepository = mock(ExecucioMassivaContingutRepository.class);
		documentService = mock(DocumentExclosResourceServiceImpl.class);
		helper = new ExecucioMassivaHelper(execucioRepository, contingutRepository, documentService);
		SecurityContextHolder.clearContext();
	}

	@After
	public void tearDown() {
		SecurityContextHolder.clearContext();
	}

	private static ExecucioMassivaEntity execucio(Long id, String creador) {
		ExecucioMassivaEntity execucio = new ExecucioMassivaEntity();
		execucio.setId(id);
		execucio.setTipus(ExecucioMassivaTipusEnum.ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS);
		execucio.setEstat(ExecucioMassivaEstatEnum.EN_CURS);
		execucio.setCreatedBy(creador);
		execucio.setDataInici(LocalDateTime.of(2026, 11, 18, 10, 0));
		return execucio;
	}

	private static ExecucioMassivaContingutEntity contingut(Long id, ExecucioMassivaEntity execucio, Long elementId) {
		ExecucioMassivaContingutEntity contingut = new ExecucioMassivaContingutEntity();
		contingut.setId(id);
		contingut.setExecucioMassiva(execucio);
		contingut.setElementTipus(ElementTipusEnum.DOCUMENT);
		contingut.setElementId(elementId);
		contingut.setEstat(ExecucioMassivaContingutEstatEnum.PENDENT);
		return contingut;
	}

	private static DocumentExclosEntity document(DocumentExclosEstatEnum estat, String missatge) {
		DocumentExclosEntity document = new DocumentExclosEntity();
		document.setEstatActualitzacio(estat);
		document.setMissatgeActualitzacio(missatge);
		return document;
	}

	// --- Tractament d'un element -------------------------------------------------------------

	@Test
	public void unElementConsultatBeQuedaFinalitzat() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));
		when(documentService.actualitzaDocument(50L)).thenReturn(document(DocumentExclosEstatEnum.OK, null));

		helper.executaContingut(10L);

		assertEquals(ExecucioMassivaContingutEstatEnum.FINALITZAT, contingut.getEstat());
		assertNotNull(contingut.getDataInici());
		assertNotNull(contingut.getDataFi());
		assertNull(contingut.getError());
		verify(contingutRepository).save(contingut);
	}

	@Test
	public void unDocumentNoTrobatNoEsUnErrorDeLExecucio() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));
		when(documentService.actualitzaDocument(50L)).thenReturn(document(DocumentExclosEstatEnum.NO_TROBAT, "No existeix"));

		helper.executaContingut(10L);

		assertEquals(ExecucioMassivaContingutEstatEnum.FINALITZAT, contingut.getEstat());
	}

	@Test
	public void unaConsultaFallidaQuedaCoErrorAmbElMotiu() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));
		when(documentService.actualitzaDocument(50L)).thenReturn(document(DocumentExclosEstatEnum.ERROR, "Timeout"));

		helper.executaContingut(10L);

		assertEquals(ExecucioMassivaContingutEstatEnum.ERROR, contingut.getEstat());
		assertEquals("Timeout", contingut.getError());
		assertNotNull(contingut.getDataFi());
	}

	@Test
	public void unErrorSenseMotiuTeUnMissatgePerDefecte() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));
		when(documentService.actualitzaDocument(50L)).thenReturn(document(DocumentExclosEstatEnum.ERROR, null));

		helper.executaContingut(10L);

		assertEquals(ExecucioMassivaContingutEstatEnum.ERROR, contingut.getEstat());
		assertFalse(contingut.getError().isEmpty());
	}

	@Test
	public void unDocumentEsborratQuedaCoError() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));
		when(documentService.actualitzaDocument(50L)).thenReturn(null);

		helper.executaContingut(10L);

		assertEquals(ExecucioMassivaContingutEstatEnum.ERROR, contingut.getEstat());
		assertTrue(contingut.getError().contains("ja no existeix"));
	}

	@Test
	public void unaExcepcioDelTractamentNoEsPropagaIQuedaCoError() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));
		when(documentService.actualitzaDocument(50L)).thenThrow(new IllegalStateException("Fallada greu"));

		helper.executaContingut(10L);

		assertEquals(ExecucioMassivaContingutEstatEnum.ERROR, contingut.getEstat());
		assertTrue(contingut.getError().contains("Fallada greu"));
		verify(contingutRepository).save(contingut);
	}

	@Test
	public void unElementQueJaNoEsPendentNoEsTornaATractar() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		contingut.setEstat(ExecucioMassivaContingutEstatEnum.FINALITZAT);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));

		helper.executaContingut(10L);

		verify(documentService, never()).actualitzaDocument(any());
		verify(contingutRepository, never()).save(any(ExecucioMassivaContingutEntity.class));
	}

	@Test
	public void creadorEsLUsuariQueHaCreatLExecucio() {
		when(execucioRepository.findById(1L)).thenReturn(Optional.of(execucio(1L, "creador")));

		assertEquals("creador", helper.creador(1L));
	}

	@Test(expected = IllegalStateException.class)
	public void creadorDUnaExecucioInexistentFalla() {
		when(execucioRepository.findById(1L)).thenReturn(Optional.empty());

		helper.creador(1L);
	}

	@Test
	public void autenticacioTeElNomDeLUsuariIPermisDeSuperusuari() {
		Authentication autenticacio = ExecucioMassivaHelper.autenticacio("u1");

		assertEquals("u1", autenticacio.getName());
		assertTrue(autenticacio.isAuthenticated());
		assertEquals("CSV_SUPER", autenticacio.getAuthorities().iterator().next().getAuthority());
	}

	@Test
	public void elsErrorsMoltLlargsEsRetallenALaMidaDeLaColumna() {
		ExecucioMassivaContingutEntity contingut = contingut(10L, execucio(1L, "u1"), 50L);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(contingut));
		StringBuilder llarg = new StringBuilder();
		for (int i = 0; i < 3000; i++) {
			llarg.append('x');
		}
		when(documentService.actualitzaDocument(50L)).thenReturn(document(DocumentExclosEstatEnum.ERROR, llarg.toString()));

		helper.executaContingut(10L);

		assertEquals(ExecucioMassivaContingutEntity.ERROR_MAX, contingut.getError().length());
	}

	@Test
	public void marcaContingutErrorNomesSiEncaraEsPendent() {
		ExecucioMassivaContingutEntity pendent = contingut(10L, execucio(1L, "u1"), 50L);
		ExecucioMassivaContingutEntity acabat = contingut(11L, execucio(1L, "u1"), 51L);
		acabat.setEstat(ExecucioMassivaContingutEstatEnum.FINALITZAT);
		when(contingutRepository.findById(10L)).thenReturn(Optional.of(pendent));
		when(contingutRepository.findById(11L)).thenReturn(Optional.of(acabat));

		helper.marcaContingutError(10L, "Error");
		helper.marcaContingutError(11L, "Error");

		assertEquals(ExecucioMassivaContingutEstatEnum.ERROR, pendent.getEstat());
		assertEquals(ExecucioMassivaContingutEstatEnum.FINALITZAT, acabat.getEstat());
		verify(contingutRepository).save(pendent);
		verify(contingutRepository, never()).save(acabat);
	}

	// --- Tancament ---------------------------------------------------------------------------

	@Test
	public void finalitzaUnaExecucioNormalCoFinalitzada() {
		ExecucioMassivaEntity execucio = execucio(1L, "u1");
		when(execucioRepository.getCancelacioData(1L)).thenReturn(null);
		when(execucioRepository.findById(1L)).thenReturn(Optional.of(execucio));

		helper.finalitza(1L, null);

		assertEquals(ExecucioMassivaEstatEnum.FINALITZADA, execucio.getEstat());
		assertNotNull(execucio.getDataFi());
		verify(execucioRepository).save(execucio);
		verify(contingutRepository, never()).cancelarPendents(any(), any(), any(), any());
		verify(contingutRepository, never()).errorPendents(any(), any(), any(), any(), any());
	}

	@Test
	public void finalitzaUnaExecucioCancelladaCancelantElsPendents() {
		ExecucioMassivaEntity execucio = execucio(1L, "u1");
		when(execucioRepository.getCancelacioData(1L)).thenReturn(LocalDateTime.now());
		when(execucioRepository.findById(1L)).thenReturn(Optional.of(execucio));

		helper.finalitza(1L, null);

		assertEquals(ExecucioMassivaEstatEnum.CANCELADA, execucio.getEstat());
		verify(contingutRepository).cancelarPendents(
				eq(1L), any(), eq(ExecucioMassivaContingutEstatEnum.PENDENT), eq(ExecucioMassivaContingutEstatEnum.CANCELAT));
	}

	@Test
	public void finalitzaUnaExecucioFallidaMarcantElsPendentsAmbElError() {
		ExecucioMassivaEntity execucio = execucio(1L, "u1");
		when(execucioRepository.getCancelacioData(1L)).thenReturn(null);
		when(execucioRepository.findById(1L)).thenReturn(Optional.of(execucio));

		helper.finalitza(1L, "Sense connexió");

		// Els elements que no s'han tractat reben el motiu; l'execució es tanca igualment
		assertEquals(ExecucioMassivaEstatEnum.FINALITZADA, execucio.getEstat());
		verify(contingutRepository).errorPendents(
				eq(1L),
				any(),
				eq("Sense connexió"),
				eq(ExecucioMassivaContingutEstatEnum.PENDENT),
				eq(ExecucioMassivaContingutEstatEnum.ERROR));
	}

	@Test(expected = IllegalStateException.class)
	public void finalitzarUnaExecucioInexistentFalla() {
		when(execucioRepository.findById(1L)).thenReturn(Optional.empty());

		helper.finalitza(1L, null);
	}
}
