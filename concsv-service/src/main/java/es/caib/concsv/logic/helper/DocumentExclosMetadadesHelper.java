package es.caib.concsv.logic.helper;

import es.caib.arxiudigital.apirest.ApiArchivoDigital;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.DocumentNode;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.Metadata;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.ResParamSearchDocument;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.RespuestaGenerica;
import es.caib.arxiudigital.apirest.CSGD.entidades.resultados.SearchDocsResult;
import es.caib.arxiudigital.apirest.facade.pojos.CabeceraPeticion;
import es.caib.arxiudigital.apirest.facade.pojos.Documento;
import es.caib.arxiudigital.apirest.facade.resultados.Resultado;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.DocumentExclosEstatEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosOrigenEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosResource;
import es.caib.concsv.logic.intf.model.DocumentExclosUbicacioEnum;
import es.caib.concsv.logic.util.StrUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Obté les dades d'un document exclòs (nom, data de captura, CSV i UUID, òrgan i origen) consultant
 * el propi document: primer a l'Arxiu Digital i, si és un CSV que l'Arxiu no té, a la custòdia antiga.
 * <p>
 * Estar a la llista d'exclosos només impedeix descarregar l'original; el document es pot consultar
 * igualment. Els serveis de consulta són beans CDI i no es poden injectar als serveis de Spring
 * (com passa amb {@link IntegracioDiagnosticHelper}), així que aquest helper llegeix la mateixa
 * configuració i fa les mateixes peticions que {@code NewDigitalArchiveService} i
 * {@code OldSaveKeepingService}, però només de metadades: no baixa el contingut del document ni
 * valida les firmes. Si es canvia la forma de consultar una integració en aquests serveis, cal
 * revisar-ho també aquí.
 * <p>
 * Mai no llança excepcions: un error de la integració és l'estat {@code ERROR} del resultat, perquè
 * no impedeixi crear o modificar el document exclòs. Les consultes no es registren al monitor
 * d'integracions ni als subsistemes: no són trànsit de la consulta pública.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentExclosMetadadesHelper {

	/** Temps màxim de la consulta d'un document. */
	static final long TIMEOUT_SEGONS = 30;

	/** UUID de l'Arxiu (Alfresco). */
	static final Pattern PATRO_UUID = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	/** Format dels CSV de l'Arxiu, com a {@code NewDigitalArchiveService.checkHash}. */
	static final Pattern PATRO_CSV_ARXIU = Pattern.compile("^[0-9a-f]{64}$");

	private static final String CUSTODIA_NO_TROBAT = "es.caib.signatura.error.ReservaNotFoundException";
	private static final int MAX_MISSATGE = 1000;

	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "concsv-document-exclos-metadades");
		thread.setDaemon(true);
		return thread;
	});

	private final ConfigHelper configHelper;

	/** Temps màxim de cada consulta; modificable per als tests. */
	long timeoutMs = TIMEOUT_SEGONS * 1000;

	/**
	 * Resultat de la consulta d'un document. {@code estat} és sempre un dels
	 * {@link DocumentExclosEstatEnum}; la resta de camps només hi són si l'estat és
	 * {@code OK}, excepte {@code missatge}, que explica el {@code NO_TROBAT} o l'{@code ERROR}.
	 */
	public static class Metadades {
		public DocumentExclosEstatEnum estat;
		public String missatge;
		public String nomDocument;
		public LocalDateTime dataCaptura;
		public String csv;
		public String uuid;
		public String organ;
		public DocumentExclosOrigenEnum origen;
		public DocumentExclosUbicacioEnum ubicacio;

		static Metadades noTrobat(String missatge) {
			Metadades metadades = new Metadades();
			metadades.estat = DocumentExclosEstatEnum.NO_TROBAT;
			metadades.missatge = missatge;
			return metadades;
		}

		static Metadades error(String missatge) {
			Metadades metadades = new Metadades();
			metadades.estat = DocumentExclosEstatEnum.ERROR;
			metadades.missatge = retalla(missatge);
			return metadades;
		}
	}

	/** Un document de l'Arxiu: només el que cal d'ell, perquè les proves no depenguin de la llibreria. */
	static class DocumentArxiu {
		String id;
		String nom;
		Map<String, Object> metadades = new HashMap<>();
	}

	/**
	 * Consulta el document identificat per {@code valor} (UUID o CSV). No llança excepcions.
	 */
	public Metadades resol(String valor) {
		String identificador = valor != null ? valor.trim() : "";
		if (identificador.isEmpty()) {
			return Metadades.error("El valor és buit");
		}
		Future<Metadades> execucio = EXECUTOR.submit(() -> resolSensePrazo(identificador));
		try {
			return execucio.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (TimeoutException ex) {
			execucio.cancel(true);
			return Metadades.error("El servei no ha respost en " + (timeoutMs / 1000) + " segons");
		} catch (ExecutionException ex) {
			Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
			String text = missatge(causa);
			log.warn("Error consultant el document exclòs {}: {}", identificador, text);
			return Metadades.error(text);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			execucio.cancel(true);
			return Metadades.error("La consulta s'ha interromput");
		}
	}

	/** La lògica de la consulta, sense límit de temps; pot llançar excepcions. */
	Metadades resolSensePrazo(String identificador) throws Exception {
		boolean arxiuActiu = consultaActiva(PropertyConfig.PROP_CONSULT_NEW_DIGITAL_ARCHIVE);
		boolean custodiaActiva = consultaActiva(PropertyConfig.PROP_CONSULT_OLD_SAFEKEEPING);
		if (PATRO_UUID.matcher(identificador).matches()) {
			// La custòdia antiga només té CSV: un UUID només pot ser a l'Arxiu.
			if (!arxiuActiu) {
				return Metadades.error("La consulta a l'Arxiu Digital no està activa");
			}
			return perUuid(identificador, null);
		}
		if (!arxiuActiu && !custodiaActiva) {
			return Metadades.error("No hi ha cap integració de consulta activa");
		}
		Metadades arxiu = null;
		if (arxiuActiu && PATRO_CSV_ARXIU.matcher(identificador).matches()) {
			arxiu = perCsvArxiu(identificador);
			if (!DocumentExclosEstatEnum.NO_TROBAT.equals(arxiu.estat) || !custodiaActiva) {
				return arxiu;
			}
		}
		if (custodiaActiva) {
			Metadades custodia = perCsvCustodia(identificador);
			// Si tampoc hi és, es manté l'explicació de l'Arxiu quan s'hi ha provat.
			if (arxiu != null && DocumentExclosEstatEnum.NO_TROBAT.equals(custodia.estat)) {
				return Metadades.noTrobat(arxiu.missatge + "; " + custodia.missatge);
			}
			return custodia;
		}
		return Metadades.noTrobat("El CSV no té el format de l'Arxiu Digital");
	}

	// --- Arxiu Digital -----------------------------------------------------------------------

	private Metadades perCsvArxiu(String csv) throws Exception {
		List<DocumentArxiu> trobats = cercaArxiuPerCsv(csv);
		if (trobats == null || trobats.isEmpty()) {
			return Metadades.noTrobat("No existeix a l'Arxiu Digital");
		}
		Set<String> enis = new LinkedHashSet<>();
		for (DocumentArxiu trobat : trobats) {
			String eniId = text(trobat.metadades.get("eni:id"));
			if (eniId != null) {
				enis.add(eniId);
			}
		}
		if (enis.size() > 1) {
			return Metadades.error("La consulta del CSV ha retornat més d'un document");
		}
		return perUuid(trobats.get(0).id, csv);
	}

	/**
	 * Metadades d'un document de l'Arxiu a partir del seu UUID.
	 *
	 * @param csv
	 *            el CSV pel qual s'ha trobat, si es coneix.
	 */
	private Metadades perUuid(String uuid, String csv) throws Exception {
		DocumentArxiu document = obteArxiu(uuid);
		if (document == null) {
			return Metadades.noTrobat("No existeix a l'Arxiu Digital");
		}
		return aDadesArxiu(document, uuid, csv);
	}

	/** Dades del document de l'Arxiu; el CSV conegut (el de la cerca) preval sobre el de les metadades. */
	static Metadades aDadesArxiu(DocumentArxiu document, String uuid, String csv) {
		Metadades metadades = new Metadades();
		metadades.estat = DocumentExclosEstatEnum.OK;
		metadades.ubicacio = DocumentExclosUbicacioEnum.ARXIU;
		metadades.uuid = document.id != null ? document.id : uuid;
		metadades.nomDocument = nomNormalitzat(document.nom);
		metadades.dataCaptura = data(document.metadades.get("eni:fecha_inicio"));
		metadades.organ = text(document.metadades.get("eni:organo"));
		metadades.origen = origen(document.metadades.get("eni:origen"));
		if (csv != null) {
			metadades.csv = csv;
		} else {
			// Com a NewDigitalArchiveService: gdib:hash als documents migrats, eni:csv als altres.
			String hash = text(document.metadades.get("gdib:hash"));
			metadades.csv = hash != null ? hash : text(document.metadades.get("eni:csv"));
		}
		return metadades;
	}

	/** Cerca per CSV a l'Arxiu: la mateixa consulta que {@code NewDigitalArchiveService.checkHash}. */
	List<DocumentArxiu> cercaArxiuPerCsv(String csv) throws Exception {
		String query = "(+TYPE:\"eni:documento\" AND @eni\\:csv:\"" + csv + "\" -ASPECT:\"gdib:borrador\" -ASPECT:\"gdib:trasladado\") "
				+ " OR (+TYPE:\"gdib:documentoMigrado\" AND @gdib\\:hash:\"" + csv + "\") ";
		SearchDocsResult result = apiArxiu().busquedaDocumentos(query, 0);
		if (result == null) {
			throw new IllegalStateException("La petició a l'API d'Arxiu Digital no s'ha processat correctament");
		}
		RespuestaGenerica<ResParamSearchDocument> resposta = result.getSearchDocumentsResult();
		String codi = resposta.getResult().getCode();
		if ("COD_001".equals(codi)) {
			return List.of();
		}
		if (!"COD_000".equals(codi)) {
			throw new IllegalStateException(codi + " - " + resposta.getResult().getDescription());
		}
		List<DocumentNode> nodes = resposta.getResParam().getDocuments();
		return nodes == null ? List.of() : nodes.stream().map(DocumentExclosMetadadesHelper::aDocumentArxiu).collect(Collectors.toList());
	}

	private static DocumentArxiu aDocumentArxiu(DocumentNode node) {
		DocumentArxiu document = new DocumentArxiu();
		document.id = node.getId();
		document.nom = node.getName();
		if (node.getMetadataCollection() != null) {
			for (Metadata metadata : node.getMetadataCollection()) {
				document.metadades.put(metadata.getQname(), metadata.getValue());
			}
		}
		return document;
	}

	/**
	 * Metadades d'un document de l'Arxiu per UUID, <b>sense contingut</b>
	 * ({@code obtenerDocumento(uuid, false)}).
	 *
	 * @return el document o {@code null} si no existeix.
	 */
	DocumentArxiu obteArxiu(String uuid) throws Exception {
		Resultado<Documento> resultat = apiArxiu().obtenerDocumento(uuid, false);
		Documento documento = resultat != null ? resultat.getElementoDevuelto() : null;
		if (documento == null) {
			return null;
		}
		DocumentArxiu document = new DocumentArxiu();
		document.id = documento.getId();
		document.nom = documento.getName();
		if (documento.getMetadataCollection() != null) {
			document.metadades.putAll(documento.getMetadataCollection());
		}
		return document;
	}

	private ApiArchivoDigital apiArxiu() {
		// Com a la resta d'integracions: només el fitxer de propietats, mai la base de dades.
		CabeceraPeticion cabecera = new CabeceraPeticion();
		cabecera.setServiceVersion(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_VERSION));
		cabecera.setCodiAplicacion(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_APP_CLIENT));
		cabecera.setUsuarioSeguridad(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_USERNAME));
		cabecera.setPasswordSeguridad(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_PASSWORD));
		cabecera.setOrganizacion(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_ORG));
		cabecera.setNombreUsuario("$concsv$");
		cabecera.setNombreSolicitante("Backoffice");
		cabecera.setDocumentoSolicitante("Backoffice");
		cabecera.setNombreProcedimiento("Dades dels documents exclosos");
		ApiArchivoDigital api = new ApiArchivoDigital(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_ENDPOINT), cabecera);
		api.setTrazas(false);
		return api;
	}

	// --- Custòdia antiga ---------------------------------------------------------------------

	private Metadades perCsvCustodia(String csv) throws Exception {
		String xml;
		try {
			xml = xmlCustodia(csv);
		} catch (Exception ex) {
			if (ex.getMessage() != null && ex.getMessage().contains(CUSTODIA_NO_TROBAT)) {
				return Metadades.noTrobat("No existeix a la custòdia antiga");
			}
			throw ex;
		}
		return aDadesCustodia(xml, csv);
	}

	/** XML de {@code consultarCustodiaXMLPorHash}: només es llegeix, no es baixa el PDF. */
	String xmlCustodia(String csv) throws Exception {
		String endpoint = fitxer(PropertyConfig.PROP_OLD_SAVEKEEPING_ENDPOINT);
		Service service = new Service();
		Call call = (Call) service.createCall();
		call.setTargetEndpointAddress(new URL(endpoint));
		call.setOperationName(new QName(endpoint, "consultarCustodiaXMLPorHash"));
		// Com al servei: el timeout es pot canviar des del backoffice.
		Integer timeout = timeoutCustodia();
		if (timeout != null) {
			call.setTimeout(timeout);
		}
		byte[] resposta = (byte[]) call.invoke(new Object[] { csv });
		return new String(resposta);
	}

	/** Dades del XML de custòdia, com les llegeix {@code OldSaveKeepingService.checkHash}. */
	static Metadades aDadesCustodia(String xml, String csv) throws Exception {
		Document document = DocumentHelper.parseText(xml);
		Node correcte = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:CustodiaOK");
		if (correcte == null || !"S".equals(correcte.getText())) {
			// Com a HashService: una custòdia no correcta és un document que no existeix.
			return Metadades.noTrobat("La custòdia antiga no el té com a correcte");
		}
		Metadades metadades = new Metadades();
		metadades.estat = DocumentExclosEstatEnum.OK;
		metadades.ubicacio = DocumentExclosUbicacioEnum.CUSTODIA_ANTIGA;
		metadades.csv = csv;
		Node nom = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:Documento/vcd:Nombre");
		String nomText = nom != null ? nom.getText() : null;
		if (nomText != null && !nomText.isEmpty()) {
			String normalitzat = nomNormalitzat(nomText);
			// Com al servei: a la custòdia només hi ha PDF.
			metadades.nomDocument = normalitzat.endsWith(".pdf") ? normalitzat : normalitzat + ".pdf";
		}
		for (Node segell : document.selectNodes("/vcd:ViewCustodiaData/vcd:Sellos/vcd:Sello")) {
			Node tipus = segell.selectSingleNode("vcd:TipoSello");
			Node fecha = segell.selectSingleNode("vcd:FechaSello");
			if (tipus != null && "CUSTODIA".equals(tipus.getText()) && fecha != null) {
				// Format: 31/03/2020 13:48 (ja és hora local)
				try {
					metadades.dataCaptura = new SimpleDateFormat("dd/MM/yyyy HH:mm").parse(fecha.getText().trim()).toInstant().
							atZone(ZoneId.systemDefault()).toLocalDateTime();
				} catch (ParseException ex) {
					log.debug("Data de segell de custòdia no vàlida: {}", fecha.getText());
				}
			}
		}
		return metadades;
	}

	private Integer timeoutCustodia() {
		String timeout = configHelper.getConfig(PropertyConfig.PROP_OLD_SAVEKEEPING_TIMEOUT);
		if (timeout == null || timeout.trim().isEmpty()) {
			return null;
		}
		try {
			return Integer.valueOf(timeout.trim());
		} catch (NumberFormatException ex) {
			log.warn("El valor del timeout de la custòdia antiga no és un enter: {}", timeout);
			return null;
		}
	}

	// --- Utilitats ---------------------------------------------------------------------------

	/** Indica si la consulta a la integració és activa (propietat editable des del backoffice). */
	boolean consultaActiva(String propietat) {
		return ConfigValues.isTrue(configHelper.getConfig(propietat));
	}

	private String fitxer(String key) {
		String value = configHelper.getEnvironmentProperty(key, null);
		if (value == null) {
			throw new IllegalStateException("La propietat " + key + " no està definida");
		}
		return value;
	}

	/** El nom es normalitza com ho fan els serveis de consulta, per veure el mateix que el front. */
	static String nomNormalitzat(String nom) {
		return nom == null || nom.isEmpty() ? null : StrUtils.normalizeString(nom);
	}

	/** Origen ENI: 0 és ciutadà i 1 és administració; qualsevol altre valor es deixa sense origen. */
	static DocumentExclosOrigenEnum origen(Object valor) {
		String text = text(valor);
		if ("0".equals(text)) {
			return DocumentExclosOrigenEnum.CIUTADA;
		}
		if ("1".equals(text)) {
			return DocumentExclosOrigenEnum.ADMINISTRACIO;
		}
		return null;
	}

	/** Valor d'una metadada com a text: els enters en text, les llistes separades per comes. */
	static String text(Object valor) {
		if (valor == null) {
			return null;
		}
		String text = valor instanceof List
				? ((List<?>) valor).stream().map(String::valueOf).collect(Collectors.joining(", "))
				: String.valueOf(valor);
		return text.trim().isEmpty() ? null : text.trim();
	}

	/**
	 * Data d'una metadada de l'Arxiu: pot ser una {@link Date} o un text ISO-8601
	 * ({@code 2020-01-31T10:00:00.000+01:00}); en hora local del servidor. Si no es pot llegir,
	 * {@code null}: una data il·legible no ha d'invalidar la resta de dades.
	 */
	static LocalDateTime data(Object valor) {
		if (valor == null) {
			return null;
		}
		if (valor instanceof Date) {
			return ((Date) valor).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		}
		String text = String.valueOf(valor).trim();
		if (text.isEmpty()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(text).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
		} catch (RuntimeException ex) {
			try {
				return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").parse(text).toInstant().
						atZone(ZoneId.systemDefault()).toLocalDateTime();
			} catch (ParseException ex2) {
				log.debug("Data de l'Arxiu no vàlida: {}", text);
				return null;
			}
		}
	}

	/** Text de l'error per mostrar: classe i missatge, sense la contrasenya de l'Arxiu. */
	private String missatge(Throwable ex) {
		String text = ex.getClass().getSimpleName() + (ex.getMessage() != null ? ": " + ex.getMessage() : "");
		String contrasenya = configHelper.getEnvironmentProperty(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_PASSWORD, null);
		if (contrasenya != null && !contrasenya.isEmpty()) {
			text = text.replace(contrasenya, "***");
		}
		return retalla(text);
	}

	/** Acota el text a la mida de la columna {@code MISSATGE_ACTUALITZACIO}. */
	static String retalla(String text) {
		if (text == null || text.length() <= MAX_MISSATGE) {
			return text;
		}
		return text.substring(0, MAX_MISSATGE - 3) + "...";
	}

}
