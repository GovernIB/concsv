package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.model.ElementTipusEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaContingutEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.repository.DocumentExclosRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves de {@link ExecucioMassivaCreacioHelper}.
 */
public class ExecucioMassivaCreacioHelperTest {

	private static final ExecucioMassivaTipusEnum TIPUS = ExecucioMassivaTipusEnum.ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS;

	private ExecucioMassivaRepository execucioRepository;
	private ExecucioMassivaContingutRepository contingutRepository;
	private DocumentExclosRepository documentRepository;
	private ExecucioMassivaCreacioHelper helper;

	@Before
	public void setUp() {
		execucioRepository = mock(ExecucioMassivaRepository.class);
		contingutRepository = mock(ExecucioMassivaContingutRepository.class);
		documentRepository = mock(DocumentExclosRepository.class);
		helper = new ExecucioMassivaCreacioHelper(execucioRepository, contingutRepository, documentRepository);
		// El repositori assigna l'identificador en desar
		doAnswer(invocation -> {
			ExecucioMassivaEntity execucio = invocation.getArgument(0);
			execucio.setId(100L);
			return execucio;
		}).when(execucioRepository).save(any(ExecucioMassivaEntity.class));
		when(contingutRepository.findElementIdsEnExecucioOberta(any(), anyCollection(), any(), any()))
				.thenReturn(new ArrayList<>());
	}

	private static DocumentExclosEntity document(Long id, String valor) {
		DocumentExclosEntity document = new DocumentExclosEntity();
		document.setId(id);
		document.setValor(valor);
		return document;
	}

	@SuppressWarnings("unchecked")
	private List<ExecucioMassivaContingutEntity> continguts() {
		ArgumentCaptor<List<ExecucioMassivaContingutEntity>> captor = ArgumentCaptor.forClass(List.class);
		verify(contingutRepository).saveAll(captor.capture());
		return captor.getValue();
	}

	@Test
	public void creaUnaExecucioPendentAmbUnElementPerDocumentEnOrdre() {
		when(documentRepository.findAllById(Arrays.asList(3L, 1L, 2L)))
				.thenReturn(Arrays.asList(document(1L, "uuid-1"), document(2L, "uuid-2"), document(3L, "uuid-3")));

		ExecucioMassivaCreacioHelper.ResultatCreacio resultat =
				helper.crea(TIPUS, ElementTipusEnum.DOCUMENT, Arrays.asList(3L, 1L, 2L), true);

		assertEquals(Long.valueOf(100L), resultat.execucioId);
		assertEquals(3, resultat.total);
		assertEquals(0, resultat.omesos);
		ArgumentCaptor<ExecucioMassivaEntity> execucio = ArgumentCaptor.forClass(ExecucioMassivaEntity.class);
		verify(execucioRepository).save(execucio.capture());
		assertEquals(TIPUS, execucio.getValue().getTipus());
		assertEquals(ExecucioMassivaEstatEnum.PENDENT, execucio.getValue().getEstat());
		assertTrue(execucio.getValue().isEnviarCorreu());
		assertEquals(null, execucio.getValue().getDataFi());

		List<ExecucioMassivaContingutEntity> continguts = continguts();
		assertEquals(Arrays.asList(3L, 1L, 2L), continguts.stream().map(ExecucioMassivaContingutEntity::getElementId).collect(Collectors.toList()));
		assertEquals(Arrays.asList(0, 1, 2), continguts.stream().map(ExecucioMassivaContingutEntity::getOrdre).collect(Collectors.toList()));
		assertEquals("uuid-3", continguts.get(0).getElementNom());
		for (ExecucioMassivaContingutEntity contingut : continguts) {
			assertEquals(ExecucioMassivaContingutEstatEnum.PENDENT, contingut.getEstat());
			assertEquals(ElementTipusEnum.DOCUMENT, contingut.getElementTipus());
			assertEquals(execucio.getValue(), contingut.getExecucioMassiva());
		}
	}

	@Test
	public void ometElsRepetitsILIdsQueJaNoExisteixen() {
		// El 9 s'ha esborrat; l'1 apareix dues vegades
		when(documentRepository.findAllById(Arrays.asList(1L, 9L))).thenReturn(Arrays.asList(document(1L, "uuid-1")));

		ExecucioMassivaCreacioHelper.ResultatCreacio resultat =
				helper.crea(TIPUS, ElementTipusEnum.DOCUMENT, Arrays.asList(1L, 1L, 9L), false);

		assertEquals(1, resultat.total);
		assertEquals(2, resultat.omesos);
		assertEquals(1, continguts().size());
	}

	@Test
	public void ometElsDocumentsQueJaSonPendentsEnUnaAltraExecucio() {
		when(contingutRepository.findElementIdsEnExecucioOberta(
				eq(ElementTipusEnum.DOCUMENT), anyCollection(), eq(TIPUS), eq(ExecucioMassivaContingutEstatEnum.PENDENT)))
				.thenReturn(Collections.singletonList(2L));
		when(documentRepository.findAllById(Arrays.asList(1L, 3L)))
				.thenReturn(Arrays.asList(document(1L, "a"), document(3L, "c")));

		ExecucioMassivaCreacioHelper.ResultatCreacio resultat =
				helper.crea(TIPUS, ElementTipusEnum.DOCUMENT, Arrays.asList(1L, 2L, 3L), true);

		assertEquals(2, resultat.total);
		assertEquals(1, resultat.omesos);
		assertEquals(Arrays.asList(1L, 3L), continguts().stream().map(ExecucioMassivaContingutEntity::getElementId).collect(Collectors.toList()));
	}

	@Test
	public void siNoQuedaCapElementNoCreaRes() {
		when(documentRepository.findAllById(Collections.singletonList(9L))).thenReturn(new ArrayList<>());

		ExecucioMassivaCreacioHelper.ResultatCreacio resultat =
				helper.crea(TIPUS, ElementTipusEnum.DOCUMENT, Collections.singletonList(9L), true);

		assertNull(resultat.execucioId);
		assertEquals(0, resultat.total);
		assertEquals(1, resultat.omesos);
		verify(execucioRepository, never()).save(any(ExecucioMassivaEntity.class));
		verify(contingutRepository, never()).saveAll(any());
	}

	@Test
	public void unaSeleccioBuidaONulaNoCreaRes() {
		assertNull(helper.crea(TIPUS, ElementTipusEnum.DOCUMENT, null, true).execucioId);
		assertNull(helper.crea(TIPUS, ElementTipusEnum.DOCUMENT, new ArrayList<>(), true).execucioId);
		verify(execucioRepository, never()).save(any(ExecucioMassivaEntity.class));
	}

	@Test
	public void consultaPerBlocsPerNoSuperarElLimitDelIn() {
		List<Long> ids = LongStream.rangeClosed(1, ExecucioMassivaCreacioHelper.MIDA_BLOC_IN * 2 + 1).boxed().collect(Collectors.toList());
		when(documentRepository.findAllById(any())).thenAnswer(invocation -> {
			List<DocumentExclosEntity> documents = new ArrayList<>();
			for (Long id : (Iterable<Long>) invocation.getArgument(0)) {
				documents.add(document(id, "v" + id));
			}
			return documents;
		});

		ExecucioMassivaCreacioHelper.ResultatCreacio resultat = helper.crea(TIPUS, ElementTipusEnum.DOCUMENT, ids, false);

		assertEquals(ids.size(), resultat.total);
		assertFalse(resultat.execucioId == null);
		// Tres blocs de com a màxim MIDA_BLOC_IN per a la comprovació de pendents i tres per als noms
		verify(contingutRepository, times(3)).findElementIdsEnExecucioOberta(any(), anyCollection(), any(), any());
		verify(documentRepository, times(3)).findAllById(any());
	}

}
