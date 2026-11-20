package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.helper.ConfigHelper;
import es.caib.concsv.logic.helper.DocumentExclosMetadadesHelper;
import es.caib.concsv.logic.helper.ExecucioMassivaCreacioHelper;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.base.exception.ReportGenerationException;
import es.caib.concsv.logic.intf.base.model.DownloadableFile;
import es.caib.concsv.logic.intf.base.model.ReportFileType;
import es.caib.concsv.logic.intf.model.DocumentExclosEstatEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosResource;
import es.caib.concsv.logic.intf.model.DocumentExclosUbicacioEnum;
import es.caib.concsv.logic.intf.model.ElementTipusEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.logic.intf.resourceservice.DocumentExclosResourceService;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import es.caib.concsv.persist.repository.DocumentExclosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementació del servei de gestió dels documents exclosos de la descàrrega de l'original.
 * <p>
 * La unicitat del valor la garanteix la restricció {@code CSV_DOCUMENT_EXCLOS_VALOR_UK} de la base
 * de dades, com a les entitats: l'error es mostra amb el missatge de {@code concsv-service-messages}.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentExclosResourceServiceImpl extends BaseMutableResourceService<DocumentExclosResource, Long, DocumentExclosEntity> implements DocumentExclosResourceService {

	/** Nom del fitxer descarregat. */
	static final String FITXER_EXPORTACIO = "documents-exclosos.txt";
	/** Única extensió admesa als fitxers a importar. */
	static final String EXTENSIO_IMPORTACIO = ".txt";
	/** Mida dels blocs d'eliminació: Oracle no admet més de 1000 elements a un {@code IN}. */
	static final int MIDA_BLOC_ELIMINACIO = 500;

	private final DocumentExclosRepository documentExclosRepository;
	private final DocumentExclosMetadadesHelper metadadesHelper;
	private final ConfigHelper configHelper;
	private final ExecucioMassivaCreacioHelper execucioMassivaCreacioHelper;

	@PostConstruct
	public void init() {
		register(DocumentExclosResource.REPORT_EXPORTAR_CODE, new ExportarReportGenerator());
		register(DocumentExclosResource.ACTION_IMPORTAR_CODE, new ImportarActionExecutor());
		register(DocumentExclosResource.ACTION_ELIMINAR_MASSIU_CODE, new EliminarMassiuActionExecutor());
		register(DocumentExclosResource.ACTION_ACTUALITZAR_CODE, new ActualitzarActionExecutor());
		register(DocumentExclosResource.ACTION_ACTUALITZAR_MASSIU_CODE, new ActualitzarMassiuActionExecutor());
	}

	@Override
	protected void beforeCreateEntity(
			DocumentExclosEntity entity,
			DocumentExclosResource resource,
			Map<String, AnswerRequiredException.AnswerValue> answers) {
		normalitzaValor(resource);
	}

	@Override
	protected void beforeUpdateEntity(
			DocumentExclosEntity entity,
			DocumentExclosResource resource,
			Map<String, AnswerRequiredException.AnswerValue> answers) {
		normalitzaValor(resource);
	}

	/**
	 * Les dades del document s'omplen en crear, <b>després</b> que el recurs s'hagi copiat a l'entitat:
	 * així no se sobreescriuen i el que hagi enviat el client no hi val. Si la consulta falla es crea
	 * igualment, amb l'estat {@code ERROR}.
	 */
	@Override
	protected void beforeCreateSave(
			DocumentExclosEntity entity,
			DocumentExclosResource resource,
			Map<String, AnswerRequiredException.AnswerValue> answers) {
		aplica(entity, metadadesHelper.resol(entity.getValor()), false);
	}

	/**
	 * En modificar es torna a consultar sempre, encara que el valor no hagi canviat: serveix també
	 * per refrescar-les i descarta el que hagi enviat el client.
	 */
	@Override
	protected void beforeUpdateSave(
			DocumentExclosEntity entity,
			DocumentExclosResource resource,
			Map<String, AnswerRequiredException.AnswerValue> answers) {
		aplica(entity, metadadesHelper.resol(entity.getValor()), false);
	}

	/** L'enllaç al front públic no es persisteix: es calcula amb el CSV i la URL de consulta. */
	@Override
	protected void afterConversion(DocumentExclosEntity entity, DocumentExclosResource resource) {
		resource.setEnllac(construeixEnllac(
				configHelper.getEnvironmentProperty(PropertyConfig.PROP_QUERY_URL, null),
				entity.getCsv()));
	}

	/**
	 * URL de consulta del document al front: la mateixa que els serveis de consulta, la URL base
	 * ({@code es.caib.concsv.query.url}, que pot acabar en {@code /view/} o en {@code ?hash=}) amb el
	 * CSV al final. Sense CSV (encara no consultat, o no trobat) o sense URL base no hi ha enllaç.
	 */
	static String construeixEnllac(String urlBase, String csv) {
		if (urlBase == null || urlBase.trim().isEmpty() || csv == null || csv.trim().isEmpty()) {
			return null;
		}
		try {
			return urlBase.trim() + URLEncoder.encode(csv.trim(), StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
	 * Copia el resultat de la consulta a l'entitat i en deixa constància (data i estat).
	 *
	 * @param conservaEnError
	 *            si és cert i la consulta ha fallat ({@code ERROR}), es mantenen les dades que ja
	 *            tenia el document: un error passatger no n'ha d'esborrar les bones. Només s'hi pot
	 *            confiar si l'entitat ve de la base de dades (no d'un recurs enviat pel client).
	 */
	void aplica(DocumentExclosEntity entity, DocumentExclosMetadadesHelper.Metadades metadades, boolean conservaEnError) {
		entity.setDataActualitzacio(LocalDateTime.now());
		entity.setEstatActualitzacio(metadades.estat);
		entity.setMissatgeActualitzacio(metadades.missatge);
		if (conservaEnError && DocumentExclosEstatEnum.ERROR.equals(metadades.estat)) {
			return;
		}
		entity.setNomDocument(metadades.nomDocument);
		entity.setDataCaptura(metadades.dataCaptura);
		entity.setCsv(metadades.csv);
		entity.setUuid(metadades.uuid);
		entity.setOrgan(metadades.organ);
		entity.setOrigen(metadades.origen);
		entity.setUbicacio(metadades.ubicacio);
	}

	/** Torna a consultar les dades d'una fila i les desa. */
	DocumentExclosResource.ResultatActualitzacio actualitza(DocumentExclosEntity entity) {
		DocumentExclosMetadadesHelper.Metadades metadades = metadadesHelper.resol(entity.getValor());
		aplica(entity, metadades, true);
		documentExclosRepository.save(entity);
		return resultat(metadades.estat);
	}

	/**
	 * Torna a consultar les dades d'un document, per identificador. És el que fa el procés en segon pla
	 * de les execucions massives per a cada element: si la consulta falla, l'error queda a l'estat de
	 * l'entitat (mai no es propaga) i se'n mantenen les dades que ja tenia.
	 *
	 * @return el document actualitzat, o {@code null} si ja no existeix (esborrat des que es va crear l'execució).
	 */
	public DocumentExclosEntity actualitzaDocument(Long id) {
		return documentExclosRepository.findById(id).map(entitat -> {
			actualitza(entitat);
			return entitat;
		}).orElse(null);
	}

	/**
	 * Crea l'execució massiva que torna a consultar, en segon pla, les dades dels documents indicats
	 * (sense avís per correu per ara, vegeu el TODO d'aquest mètode).
	 */
	DocumentExclosResource.ResultatActualitzacioMassiva programaActualitzacio(List<Long> ids) {
		ExecucioMassivaCreacioHelper.ResultatCreacio creada = execucioMassivaCreacioHelper.crea(
				ExecucioMassivaTipusEnum.ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS,
				ElementTipusEnum.DOCUMENT,
				ids,
				false); // TODO: posar-ho a true si s'implementa l'enviament del correu a l'usuari que crea l'execució
		return new DocumentExclosResource.ResultatActualitzacioMassiva(creada.execucioId, creada.total, creada.omesos);
	}

	private static DocumentExclosResource.ResultatActualitzacio resultat(DocumentExclosEstatEnum estat) {
		boolean ok = DocumentExclosEstatEnum.OK == estat;
		boolean noTrobat = DocumentExclosEstatEnum.NO_TROBAT == estat;
		return new DocumentExclosResource.ResultatActualitzacio(
				ok ? 1 : 0,
				noTrobat ? 1 : 0,
				ok || noTrobat ? 0 : 1);
	}

	/** Comprova els identificadors d'una actualització massiva. @return el motiu del rebuig, o {@code null} si són vàlids. */
	static String motiuRebuigActualitzacio(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return "No s'ha seleccionat cap document";
		}
		return null;
	}

	/** Elimina els espais del principi i del final, que no formen part de l'UUID o del CSV. */
	private void normalitzaValor(DocumentExclosResource resource) {
		if (resource.getValor() != null) {
			resource.setValor(resource.getValor().trim());
		}
	}

	/**
	 * Valors d'un fitxer de text: un per línia, en UTF-8 (amb o sense BOM). S'ignoren els espais del
	 * principi i del final, les línies buides i les que comencen per {@code #}; els valors repetits es
	 * compten a {@code repetits} i els de més de {@value DocumentExclosResource#VALOR_MAX} caràcters
	 * a {@code descartats}.
	 */
	static class ContingutFitxer {
		final Set<String> valors = new LinkedHashSet<>();
		int repetits;
		int descartats;
	}

	static ContingutFitxer analitzaFitxer(byte[] contingut) {
		ContingutFitxer resultat = new ContingutFitxer();
		if (contingut == null) {
			return resultat;
		}
		String text = new String(contingut, StandardCharsets.UTF_8);
		if (text.startsWith("﻿")) {
			text = text.substring(1);
		}
		for (String linia : text.split("\\r?\\n")) {
			String valor = linia.trim();
			if (valor.isEmpty() || valor.startsWith("#")) {
				continue;
			}
			if (valor.length() > DocumentExclosResource.VALOR_MAX) {
				resultat.descartats++;
			} else if (!resultat.valors.add(valor)) {
				resultat.repetits++;
			}
		}
		return resultat;
	}

	/**
	 * Comprova que el fitxer a importar és un fitxer de text: extensió {@code .txt}, codificat en UTF-8
	 * i sense caràcters de control.
	 *
	 * @return el motiu del rebuig, o {@code null} si el fitxer és vàlid.
	 */
	static String motiuRebuigFitxer(String nom, byte[] contingut) {
		if (nom == null || !nom.toLowerCase(Locale.ROOT).endsWith(EXTENSIO_IMPORTACIO)) {
			return "El fitxer ha de ser un fitxer de text amb l'extensió " + EXTENSIO_IMPORTACIO;
		}
		if (contingut == null) {
			return "El fitxer no té contingut";
		}
		String text;
		try {
			text = StandardCharsets.UTF_8.newDecoder().
					onMalformedInput(CodingErrorAction.REPORT).
					onUnmappableCharacter(CodingErrorAction.REPORT).
					decode(ByteBuffer.wrap(contingut)).
					toString();
		} catch (CharacterCodingException ex) {
			return "El fitxer no és un fitxer de text en UTF-8";
		}
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c < ' ' && c != '\n' && c != '\r' && c != '\t') {
				return "El fitxer no és un fitxer de text: conté caràcters de control";
			}
		}
		return null;
	}

	/** Afegeix els valors del fitxer que encara no són a la taula; no esborra res. */
	DocumentExclosResource.ResultatImportacio importa(byte[] contingut) {
		ContingutFitxer fitxer = analitzaFitxer(contingut);
		Set<String> existents = new HashSet<>(documentExclosRepository.findAllValors());
		List<DocumentExclosEntity> nous = new ArrayList<>();
		int jaExistents = fitxer.repetits;
		for (String valor : fitxer.valors) {
			if (existents.contains(valor)) {
				jaExistents++;
			} else {
				DocumentExclosEntity entitat = new DocumentExclosEntity();
				entitat.setValor(valor);
				nous.add(entitat);
			}
		}
		documentExclosRepository.saveAll(nous);
		log.info("Importats {} documents exclosos ({} ja existents o repetits, {} descartats)",
				nous.size(), jaExistents, fitxer.descartats);
		return new DocumentExclosResource.ResultatImportacio(nous.size(), jaExistents, fitxer.descartats);
	}

	/** Esborra els identificadors en blocs, per no superar el límit d'elements d'un {@code IN} d'Oracle. */
	void elimina(List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return;
		}
		List<Long> sensePendents = ids.stream().distinct().collect(Collectors.toList());
		for (int i = 0; i < sensePendents.size(); i += MIDA_BLOC_ELIMINACIO) {
			documentExclosRepository.deleteAllByIdInBatch(
					sensePendents.subList(i, Math.min(i + MIDA_BLOC_ELIMINACIO, sensePendents.size())));
		}
	}

	/** Tots els valors, ordenats i en el format del fitxer d'exportació. */
	static byte[] contingutExportacio(List<?> valors) {
		String text = valors.stream().map(String::valueOf).collect(Collectors.joining("\n"));
		return (valors.isEmpty() ? text : text + "\n").getBytes(StandardCharsets.UTF_8);
	}

	/** Genera el fitxer {@code documents-exclosos.txt} amb tots els valors. */
	private class ExportarReportGenerator implements ReportGenerator<DocumentExclosEntity, Serializable, String> {

		@Override
		public void onChange(
				Serializable id,
				Serializable previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				Serializable target) {
			// Sense formulari no hi ha cap camp que pugui canviar.
		}

		@Override
		public List<String> generateData(
				String code,
				DocumentExclosEntity entity,
				Serializable params) throws ReportGenerationException {
			return documentExclosRepository.findAllValors();
		}

		@Override
		public DownloadableFile generateFile(
				String code,
				List<?> data,
				ReportFileType fileType,
				OutputStream out) {
			return DownloadableFile.builder().
					name(FITXER_EXPORTACIO).
					contentType("text/plain; charset=UTF-8").
					content(contingutExportacio(data)).
					build();
		}

	}

	/** Importa els valors d'un fitxer de text. */
	private class ImportarActionExecutor
			implements ActionExecutor<DocumentExclosEntity, DocumentExclosResource.FormImportar, DocumentExclosResource.ResultatImportacio> {

		@Override
		public void onChange(
				Serializable id,
				DocumentExclosResource.FormImportar previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				DocumentExclosResource.FormImportar target) {
			// El formulari no té cap camp que depengui dels altres.
		}

		@Override
		public DocumentExclosResource.ResultatImportacio exec(
				String code,
				DocumentExclosEntity entity,
				DocumentExclosResource.FormImportar params) throws ActionExecutionException {
			String motiu = motiuRebuigFitxer(params.getFitxer().getName(), params.getFitxer().getContent());
			if (motiu != null) {
				throw new ActionExecutionException(DocumentExclosResource.class, null, code, motiu);
			}
			return importa(params.getFitxer().getContent());
		}

	}

	/** Torna a consultar les dades del document d'una fila. */
	private class ActualitzarActionExecutor
			implements ActionExecutor<DocumentExclosEntity, Serializable, DocumentExclosResource.ResultatActualitzacio> {

		@Override
		public void onChange(
				Serializable id,
				Serializable previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				Serializable target) {
			// Sense formulari no hi ha cap camp que pugui canviar.
		}

		@Override
		public DocumentExclosResource.ResultatActualitzacio exec(
				String code,
				DocumentExclosEntity entity,
				Serializable params) throws ActionExecutionException {
			return actualitza(entity);
		}

	}

	/** Programa, en segon pla, la nova consulta de les dades del document de les files seleccionades. */
	private class ActualitzarMassiuActionExecutor
			implements ActionExecutor<DocumentExclosEntity, DocumentExclosResource.FormActualitzarMassiu, DocumentExclosResource.ResultatActualitzacioMassiva> {

		@Override
		public void onChange(
				Serializable id,
				DocumentExclosResource.FormActualitzarMassiu previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				DocumentExclosResource.FormActualitzarMassiu target) {
			// El formulari no té cap camp que depengui dels altres.
		}

		@Override
		public DocumentExclosResource.ResultatActualitzacioMassiva exec(
				String code,
				DocumentExclosEntity entity,
				DocumentExclosResource.FormActualitzarMassiu params) throws ActionExecutionException {
			String motiu = motiuRebuigActualitzacio(params.getIds());
			if (motiu != null) {
				throw new ActionExecutionException(DocumentExclosResource.class, null, code, motiu);
			}
			return programaActualitzacio(params.getIds());
		}

	}

	/** Esborra en bloc les files seleccionades. */
	private class EliminarMassiuActionExecutor
			implements ActionExecutor<DocumentExclosEntity, DocumentExclosResource.FormEliminarMassiu, Serializable> {

		@Override
		public void onChange(
				Serializable id,
				DocumentExclosResource.FormEliminarMassiu previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				DocumentExclosResource.FormEliminarMassiu target) {
			// El formulari no té cap camp que depengui dels altres.
		}

		@Override
		public Serializable exec(
				String code,
				DocumentExclosEntity entity,
				DocumentExclosResource.FormEliminarMassiu params) throws ActionExecutionException {
			elimina(params.getIds());
			return null;
		}

	}

}
