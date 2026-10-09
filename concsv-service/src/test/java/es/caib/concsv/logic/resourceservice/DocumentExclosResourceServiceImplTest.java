package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.intf.model.DocumentExclosResource;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import es.caib.concsv.persist.repository.DocumentExclosRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves de {@link DocumentExclosResourceServiceImpl}.
 */
public class DocumentExclosResourceServiceImplTest {

	private DocumentExclosRepository repository;
	private DocumentExclosResourceServiceImpl service;

	@Before
	public void setUp() {
		repository = mock(DocumentExclosRepository.class);
		service = new DocumentExclosResourceServiceImpl(repository);
	}

	private static byte[] text(String contingut) {
		return contingut.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	public void alCrearEliminaElsEspaisDelValor() {
		DocumentExclosResource resource = new DocumentExclosResource();
		resource.setValor("  12e2f405-ac11-49ed-9b93-c2f11cac41e7 \t");

		service.beforeCreateEntity(new DocumentExclosEntity(), resource, null);

		assertEquals("12e2f405-ac11-49ed-9b93-c2f11cac41e7", resource.getValor());
	}

	@Test
	public void alModificarEliminaElsEspaisDelValor() {
		DocumentExclosResource resource = new DocumentExclosResource();
		resource.setValor(" Catastrofe ");

		service.beforeUpdateEntity(new DocumentExclosEntity(), resource, null);

		assertEquals("Catastrofe", resource.getValor());
	}

	@Test
	public void ambValorNulNoFallaPerquePersisteixLaValidacio() {
		DocumentExclosResource resource = new DocumentExclosResource();

		service.beforeCreateEntity(new DocumentExclosEntity(), resource, null);

		assertNull(resource.getValor());
	}

	@Test
	public void analitzaFitxer_ignoraBuidesComentarisBomIEspais() {
		byte[] contingut = text("﻿  csv-1  \r\n\r\n# comentari\r\n   \ncsv-2\n#csv-3\n");

		DocumentExclosResourceServiceImpl.ContingutFitxer fitxer = DocumentExclosResourceServiceImpl.analitzaFitxer(contingut);

		assertEquals(Arrays.asList("csv-1", "csv-2"), new ArrayList<>(fitxer.valors));
		assertEquals(0, fitxer.repetits);
		assertEquals(0, fitxer.descartats);
	}

	@Test
	public void analitzaFitxer_comptaRepetitsIDescartatsPerLlargada() {
		String massaLlarg = String.join("", java.util.Collections.nCopies(DocumentExclosResource.VALOR_MAX + 1, "a"));
		String limit = String.join("", java.util.Collections.nCopies(DocumentExclosResource.VALOR_MAX, "b"));

		DocumentExclosResourceServiceImpl.ContingutFitxer fitxer = DocumentExclosResourceServiceImpl.analitzaFitxer(
				text("csv-1\ncsv-1\n" + massaLlarg + "\n" + limit + "\n csv-1 "));

		assertEquals(Arrays.asList("csv-1", limit), new ArrayList<>(fitxer.valors));
		assertEquals(2, fitxer.repetits);
		assertEquals(1, fitxer.descartats);
	}

	@Test
	public void analitzaFitxer_senseContingutNoDonaValors() {
		assertTrue(DocumentExclosResourceServiceImpl.analitzaFitxer(null).valors.isEmpty());
		assertTrue(DocumentExclosResourceServiceImpl.analitzaFitxer(new byte[0]).valors.isEmpty());
	}

	@Test
	public void motiuRebuigFitxer_acceptaUnTxtEnUtf8() {
		byte[] contingut = text("﻿csv-1\r\n# comentari\r\nCatastrofe-àèü\r\n\tcsv-2\r\n");

		assertNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("documents-exclosos.txt", contingut));
		assertNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("DOCUMENTS.TXT", contingut));
		assertNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("buit.txt", new byte[0]));
	}

	@Test
	public void motiuRebuigFitxer_rebutjaQualsevolExtensioQueNoSiguiTxt() {
		byte[] contingut = text("csv-1\n");

		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("document.pdf", contingut));
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("document.txt.pdf", contingut));
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("document.csv", contingut));
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("txt", contingut));
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer(null, contingut));
	}

	@Test
	public void motiuRebuigFitxer_rebutjaUnFitxerBinariAmbExtensioTxt() {
		// Capçalera d'un PDF: bytes que no són UTF-8 vàlid i caràcters de control.
		byte[] pdf = new byte[] {'%', 'P', 'D', 'F', '-', '1', '.', '4', '\n', '%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'};
		byte[] ambNuls = new byte[] {'a', 'b', 0, 'c', '\n'};

		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("fals.txt", pdf));
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("fals.txt", ambNuls));
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("fals.txt", null));
	}

	@Test
	public void motiuRebuigFitxer_rebutjaUnTextEnLatin1() {
		byte[] latin1 = "Catàstrofe\n".getBytes(StandardCharsets.ISO_8859_1);

		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigFitxer("latin1.txt", latin1));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void importa_nomesAfegeixElsQueNoHiSon() {
		when(repository.findAllValors()).thenReturn(Arrays.asList("csv-1", "csv-2"));

		DocumentExclosResource.ResultatImportacio resultat = service.importa(text("csv-2\ncsv-3\ncsv-3\ncsv-4\n"));

		ArgumentCaptor<List<DocumentExclosEntity>> desats = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(desats.capture());
		assertEquals(
				Arrays.asList("csv-3", "csv-4"),
				desats.getValue().stream().map(DocumentExclosEntity::getValor).collect(Collectors.toList()));
		assertEquals(2, resultat.getAfegits());
		assertEquals(2, resultat.getJaExistents()); // csv-2 ja hi era i csv-3 estava repetit
		assertEquals(0, resultat.getDescartats());
		verify(repository, never()).deleteAllByIdInBatch(anyIterable());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void elimina_parteixElsIdentificadorsEnBlocs() {
		List<Long> ids = LongStream.rangeClosed(1, 1200).boxed().collect(Collectors.toList());

		service.elimina(ids);

		ArgumentCaptor<Iterable<Long>> blocs = ArgumentCaptor.forClass(Iterable.class);
		verify(repository, times(3)).deleteAllByIdInBatch(blocs.capture());
		List<Integer> mides = blocs.getAllValues().stream().
				map(b -> ((List<Long>) b).size()).
				collect(Collectors.toList());
		assertEquals(Arrays.asList(500, 500, 200), mides);
	}

	@Test
	public void elimina_senseIdentificadorsNoFaRes() {
		service.elimina(null);
		service.elimina(new ArrayList<>());

		verify(repository, never()).deleteAllByIdInBatch(anyIterable());
	}

	@Test
	public void contingutExportacio_unValorPerLiniaAcabatEnSaltDeLinia() {
		assertEquals("a\nb\n", new String(
				DocumentExclosResourceServiceImpl.contingutExportacio(Arrays.asList("a", "b")),
				StandardCharsets.UTF_8));
		assertEquals(0, DocumentExclosResourceServiceImpl.contingutExportacio(new ArrayList<>()).length);
	}

}
