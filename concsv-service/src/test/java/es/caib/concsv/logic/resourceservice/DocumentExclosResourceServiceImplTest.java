package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.helper.ConfigHelper;
import es.caib.concsv.logic.helper.DocumentExclosMetadadesHelper;
import es.caib.concsv.logic.helper.ExecucioMassivaCreacioHelper;
import es.caib.concsv.logic.intf.model.DocumentExclosEstatEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosOrigenEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosResource;
import es.caib.concsv.logic.intf.model.DocumentExclosUbicacioEnum;
import es.caib.concsv.logic.intf.model.ElementTipusEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import es.caib.concsv.persist.repository.DocumentExclosRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
	private DocumentExclosMetadadesHelper metadadesHelper;
	private ConfigHelper configHelper;
	private ExecucioMassivaCreacioHelper creacioHelper;
	private DocumentExclosResourceServiceImpl service;

	@Before
	public void setUp() {
		repository = mock(DocumentExclosRepository.class);
		metadadesHelper = mock(DocumentExclosMetadadesHelper.class);
		configHelper = mock(ConfigHelper.class);
		creacioHelper = mock(ExecucioMassivaCreacioHelper.class);
		service = new DocumentExclosResourceServiceImpl(repository, metadadesHelper, configHelper, creacioHelper);
	}

	private static DocumentExclosMetadadesHelper.Metadades ok() {
		DocumentExclosMetadadesHelper.Metadades m = new DocumentExclosMetadadesHelper.Metadades();
		m.estat = DocumentExclosEstatEnum.OK;
		m.nomDocument = "Informe.pdf";
		m.dataCaptura = LocalDateTime.of(2020, 3, 31, 13, 48);
		m.csv = "a".repeat(64);
		m.uuid = "12e2f405-ac11-49ed-9b93-c2f11cac41e7";
		m.organ = "A04003003";
		m.origen = DocumentExclosOrigenEnum.ADMINISTRACIO;
		m.ubicacio = DocumentExclosUbicacioEnum.ARXIU;
		return m;
	}

	private static DocumentExclosMetadadesHelper.Metadades error() {
		DocumentExclosMetadadesHelper.Metadades m = new DocumentExclosMetadadesHelper.Metadades();
		m.estat = DocumentExclosEstatEnum.ERROR;
		m.missatge = "Timeout";
		return m;
	}

	private static DocumentExclosEntity entitat(Long id, String valor) {
		DocumentExclosEntity entitat = new DocumentExclosEntity();
		entitat.setId(id);
		entitat.setValor(valor);
		return entitat;
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

	// --- Dades del document ------------------------------------------------------------------

	@Test
	public void alCrearOmpleLesDadesDelDocumentAmbElValorJaCopiatAlEntity() {
		DocumentExclosEntity entity = entitat(null, "12e2f405-ac11-49ed-9b93-c2f11cac41e7");
		// El que enviï el client no hi val: es descarta amb el resultat de la consulta.
		entity.setNomDocument("Falsificat.pdf");
		when(metadadesHelper.resol("12e2f405-ac11-49ed-9b93-c2f11cac41e7")).thenReturn(ok());

		service.beforeCreateSave(entity, new DocumentExclosResource(), null);

		assertEquals("Informe.pdf", entity.getNomDocument());
		assertEquals(LocalDateTime.of(2020, 3, 31, 13, 48), entity.getDataCaptura());
		assertEquals("a".repeat(64), entity.getCsv());
		assertEquals("12e2f405-ac11-49ed-9b93-c2f11cac41e7", entity.getUuid());
		assertEquals("A04003003", entity.getOrgan());
		assertEquals(DocumentExclosOrigenEnum.ADMINISTRACIO, entity.getOrigen());
		assertEquals(DocumentExclosUbicacioEnum.ARXIU, entity.getUbicacio());
		assertEquals(DocumentExclosEstatEnum.OK, entity.getEstatActualitzacio());
		assertNotNull(entity.getDataActualitzacio());
		assertNull(entity.getMissatgeActualitzacio());
	}

	@Test
	public void alModificarTornaAConsultarElDocument() {
		DocumentExclosEntity entity = entitat(1L, "nou-valor");
		when(metadadesHelper.resol("nou-valor")).thenReturn(ok());

		service.beforeUpdateSave(entity, new DocumentExclosResource(), null);

		verify(metadadesHelper).resol("nou-valor");
		assertEquals("Informe.pdf", entity.getNomDocument());
	}

	@Test
	public void alCrearAmbErrorDeConsultaDeixaLesDadesBuidesIAnotaElMotiu() {
		DocumentExclosEntity entity = entitat(null, "x");
		entity.setNomDocument("Enviat pel client.pdf");
		when(metadadesHelper.resol("x")).thenReturn(error());

		service.beforeCreateSave(entity, new DocumentExclosResource(), null);

		assertEquals(DocumentExclosEstatEnum.ERROR, entity.getEstatActualitzacio());
		assertEquals("Timeout", entity.getMissatgeActualitzacio());
		assertNull(entity.getNomDocument());
	}

	@Test
	public void actualitzaUnaFilaDesaElResultat() {
		DocumentExclosEntity entity = entitat(1L, "x");
		when(metadadesHelper.resol("x")).thenReturn(ok());

		DocumentExclosResource.ResultatActualitzacio resultat = service.actualitza(entity);

		verify(repository).save(entity);
		assertEquals(1, resultat.getActualitzats());
		assertEquals(0, resultat.getErrors());
	}

	@Test
	public void actualitzaUnaFilaAmbErrorPassatgerConservaLesDadesAnteriors() {
		DocumentExclosEntity entity = entitat(1L, "x");
		entity.setNomDocument("Anterior.pdf");
		entity.setEstatActualitzacio(DocumentExclosEstatEnum.OK);
		when(metadadesHelper.resol("x")).thenReturn(error());

		DocumentExclosResource.ResultatActualitzacio resultat = service.actualitza(entity);

		assertEquals("Anterior.pdf", entity.getNomDocument());
		assertEquals(DocumentExclosEstatEnum.ERROR, entity.getEstatActualitzacio());
		assertEquals("Timeout", entity.getMissatgeActualitzacio());
		assertEquals(1, resultat.getErrors());
		assertEquals(0, resultat.getActualitzats());
	}

	@Test
	public void actualitzaUnaFilaNoTrobadaLaComptaAPart() {
		DocumentExclosEntity entity = entitat(1L, "x");
		DocumentExclosMetadadesHelper.Metadades noTrobat = new DocumentExclosMetadadesHelper.Metadades();
		noTrobat.estat = DocumentExclosEstatEnum.NO_TROBAT;
		noTrobat.missatge = "No existeix";
		when(metadadesHelper.resol("x")).thenReturn(noTrobat);

		DocumentExclosResource.ResultatActualitzacio resultat = service.actualitza(entity);

		assertEquals(1, resultat.getNoTrobats());
		assertEquals(0, resultat.getActualitzats());
		assertNull(entity.getNomDocument());
	}

	@Test
	public void actualitzaDocumentPerIdConsultaIDesaLaFila() {
		DocumentExclosEntity entity = entitat(1L, "a");
		when(repository.findById(1L)).thenReturn(Optional.of(entity));
		when(metadadesHelper.resol("a")).thenReturn(ok());

		DocumentExclosEntity resultat = service.actualitzaDocument(1L);

		assertSame(entity, resultat);
		assertEquals(DocumentExclosEstatEnum.OK, resultat.getEstatActualitzacio());
		verify(repository).save(entity);
	}

	@Test
	public void actualitzaDocumentPerIdAmbErrorLiDeixaLEstatIConservaLesDades() {
		DocumentExclosEntity entity = entitat(1L, "a");
		entity.setNomDocument("Anterior.pdf");
		when(repository.findById(1L)).thenReturn(Optional.of(entity));
		when(metadadesHelper.resol("a")).thenReturn(error());

		DocumentExclosEntity resultat = service.actualitzaDocument(1L);

		assertEquals(DocumentExclosEstatEnum.ERROR, resultat.getEstatActualitzacio());
		assertEquals("Timeout", resultat.getMissatgeActualitzacio());
		assertEquals("Anterior.pdf", resultat.getNomDocument());
	}

	@Test
	public void actualitzaDocumentPerIdEsborratRetornaNullSenseConsultarRes() {
		when(repository.findById(9L)).thenReturn(Optional.empty());

		assertNull(service.actualitzaDocument(9L));

		verify(metadadesHelper, never()).resol(org.mockito.ArgumentMatchers.anyString());
		verify(repository, never()).save(org.mockito.ArgumentMatchers.any(DocumentExclosEntity.class));
	}

	@Test
	public void programaActualitzacioCreaLExecucioIRetornaElResultat() {
		when(creacioHelper.crea(
				ExecucioMassivaTipusEnum.ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS,
				ElementTipusEnum.DOCUMENT,
				Arrays.asList(1L, 2L, 3L),
				false)).thenReturn(new ExecucioMassivaCreacioHelper.ResultatCreacio(77L, 2, 1));

		DocumentExclosResource.ResultatActualitzacioMassiva resultat = service.programaActualitzacio(Arrays.asList(1L, 2L, 3L));

		assertEquals(Long.valueOf(77L), resultat.getExecucioId());
		assertEquals(2, resultat.getTotal());
		assertEquals(1, resultat.getOmesos());
		// La petició només crea l'execució: no consulta cap document
		verify(metadadesHelper, never()).resol(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	public void motiuRebuigActualitzacio_nomesRebutjaLaSeleccioBuida() {
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigActualitzacio(null));
		assertNotNull(DocumentExclosResourceServiceImpl.motiuRebuigActualitzacio(new ArrayList<>()));
		// Ja no hi ha límit: la consulta es fa en segon pla
		assertNull(DocumentExclosResourceServiceImpl.motiuRebuigActualitzacio(
				LongStream.rangeClosed(1, 5000).boxed().collect(Collectors.toList())));
	}

	@Test
	public void importarDeixaLesFilesNovesPendentsSenseConsultarRes() {
		when(repository.findAllValors()).thenReturn(new ArrayList<>());

		service.importa(text("uuid-1\nuuid-2\n"));

		ArgumentCaptor<List<DocumentExclosEntity>> desades = ArgumentCaptor.forClass(List.class);
		verify(repository).saveAll(desades.capture());
		for (DocumentExclosEntity entitat : desades.getValue()) {
			assertEquals(DocumentExclosEstatEnum.PENDENT, entitat.getEstatActualitzacio());
			assertNull(entitat.getNomDocument());
		}
		verify(metadadesHelper, never()).resol(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	public void construeixEnllac_ambLesDuesFormesDeLaUrlBase() {
		assertEquals("http://localhost:8080/concsvfront/view/abc",
				DocumentExclosResourceServiceImpl.construeixEnllac("http://localhost:8080/concsvfront/view/", "abc"));
		assertEquals("http://host/concsvfront/view.xhtml?hash=abc",
				DocumentExclosResourceServiceImpl.construeixEnllac(" http://host/concsvfront/view.xhtml?hash= ", " abc "));
	}

	@Test
	public void construeixEnllac_senseCsvOSenseUrlBaseNoHiHaEnllac() {
		assertNull(DocumentExclosResourceServiceImpl.construeixEnllac("http://host/view/", null));
		assertNull(DocumentExclosResourceServiceImpl.construeixEnllac("http://host/view/", " "));
		assertNull(DocumentExclosResourceServiceImpl.construeixEnllac(null, "abc"));
		assertNull(DocumentExclosResourceServiceImpl.construeixEnllac("", "abc"));
	}

	@Test
	public void alConvertirOmpleLEnllacAmbElCsvResolt() {
		when(configHelper.getEnvironmentProperty(
				es.caib.concsv.logic.intf.config.PropertyConfig.PROP_QUERY_URL, null)).thenReturn("http://host/view/");
		DocumentExclosEntity entity = entitat(1L, "uuid");
		entity.setCsv("csv123");
		DocumentExclosResource resource = new DocumentExclosResource();

		service.afterConversion(entity, resource);

		assertEquals("http://host/view/csv123", resource.getEnllac());
	}


	@Test
	public void origenEsDesaALaBaseDeDadesComZeroOUn() {
		es.caib.concsv.persist.entity.DocumentExclosOrigenConverter converter =
				new es.caib.concsv.persist.entity.DocumentExclosOrigenConverter();
		assertEquals("0", converter.convertToDatabaseColumn(DocumentExclosOrigenEnum.CIUTADA));
		assertEquals("1", converter.convertToDatabaseColumn(DocumentExclosOrigenEnum.ADMINISTRACIO));
		assertNull(converter.convertToDatabaseColumn(null));
		assertEquals(DocumentExclosOrigenEnum.CIUTADA, converter.convertToEntityAttribute("0"));
		assertEquals(DocumentExclosOrigenEnum.ADMINISTRACIO, converter.convertToEntityAttribute("1"));
		assertNull(converter.convertToEntityAttribute("7"));
		assertNull(converter.convertToEntityAttribute(null));
	}

}
