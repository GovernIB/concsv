package es.caib.concsv.logic.helper;

import es.caib.arxiudigital.apirest.ApiArchivoDigital;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.RespuestaGenerica;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.ResParamSearchDocument;
import es.caib.arxiudigital.apirest.CSGD.entidades.resultados.SearchDocsResult;
import es.caib.arxiudigital.apirest.facade.pojos.CabeceraPeticion;
import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource;
import es.caib.concsv.logic.plugin.ValidateSignaturePlugin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.fundaciobit.plugins.documentconverter.openoffice.OpenOfficeDocumentConverterPlugin;
import org.fundaciobit.plugins.validatesignature.api.IValidateSignaturePlugin;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureRequest;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureResponse;
import org.fundaciobit.pluginsib.documentconverter.IDocumentConverterPlugin;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Diagnòstic ràpid de les integracions des del backoffice: fa una petició mínima, sense efectes,
 * a cada integració externa.
 * <p>
 * Els serveis d'integració són beans CDI i no es poden injectar als serveis de Spring (com passa
 * amb {@link CacheEsborratHelper}), així que aquest helper llegeix la mateixa configuració que
 * ells i construeix els seus propis clients. Si es canvia la forma de cridar una integració en un
 * servei, cal revisar també la prova corresponent d'aquí.
 * <p>
 * Una prova és correcta si el servei <b>respon</b> (un «no trobat» és una resposta); falla si hi ha
 * un error de connexió, de credencials o del plugin, o si el servei no respon en
 * {@value #TIMEOUT_SEGONS} segons. El diagnòstic no es registra al monitor ni als comptadors de
 * Comanda: no és trànsit real.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntegracioDiagnosticHelper {

	static final long TIMEOUT_SEGONS = 30;

	/** Codi que no pot existir: l'Arxiu i la custòdia responen «no trobat». */
	private static final String CODI_INEXISTENT = "0000000000000000000000000000000000000000000000000000000000000000";
	private static final String CUSTODIA_NO_TROBAT = "es.caib.signatura.error.ReservaNotFoundException";

	private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "concsv-integracio-diagnostic");
		thread.setDaemon(true);
		return thread;
	});

	private final ConfigHelper configHelper;

	/** Temps màxim de cada prova; modificable per als tests. */
	long timeoutMs = TIMEOUT_SEGONS * 1000;

	/** Codis de les integracions que es poden diagnosticar. */
	public List<String> codis() {
		return Arrays.stream(IntegracioCodiEnum.values()).map(Enum::name).collect(Collectors.toList());
	}

	/**
	 * Executa la prova d'una integració. Mai no llança excepcions: un error de la integració és el
	 * resultat del diagnòstic.
	 */
	public MonitorIntegracioResource.DiagnosticResult executa(IntegracioCodiEnum codi) {
		long t0 = System.currentTimeMillis();
		Future<String> execucio = EXECUTOR.submit(prova(codi));
		try {
			String prova = execucio.get(timeoutMs, TimeUnit.MILLISECONDS);
			return new MonitorIntegracioResource.DiagnosticResult(
					true,
					prova + " (" + (System.currentTimeMillis() - t0) + " ms)",
					null,
					null);
		} catch (TimeoutException ex) {
			execucio.cancel(true);
			return error("El servei no ha respost en " + (timeoutMs / 1000) + " segons", null);
		} catch (ExecutionException ex) {
			log.debug("Diagnòstic de la integració {} fallit", codi, ex.getCause());
			Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
			return error(missatge(causa), causa);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			execucio.cancel(true);
			return error("El diagnòstic s'ha interromput", null);
		}
	}

	/** La prova de cada integració: retorna el text de l'èxit o llança una excepció. */
	Callable<String> prova(IntegracioCodiEnum codi) {
		switch (codi) {
			case ARXIU:
				return this::provaArxiu;
			case CUSTODIA:
				return this::provaCustodia;
			case VALIDACIO_FIRMES:
				return this::provaValidacioFirmes;
			case CONVERSIO:
				return this::provaConversio;
			default:
				throw new IllegalArgumentException("Integració no diagnosticable: " + codi);
		}
	}

	/** Cerca d'un CSV inexistent: la mateixa petició que fa la consulta pública. */
	private String provaArxiu() throws Exception {
		// @ConfigProperty als serveis: només el fitxer de propietats, mai la base de dades.
		String endpoint = fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_ENDPOINT);
		CabeceraPeticion cabecera = new CabeceraPeticion();
		cabecera.setServiceVersion(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_VERSION));
		cabecera.setCodiAplicacion(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_APP_CLIENT));
		cabecera.setUsuarioSeguridad(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_USERNAME));
		cabecera.setPasswordSeguridad(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_PASSWORD));
		cabecera.setOrganizacion(fitxer(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_ORG));
		cabecera.setNombreUsuario("$concsv$");
		cabecera.setNombreSolicitante("Diagnòstic");
		cabecera.setDocumentoSolicitante("Diagnòstic");
		cabecera.setNombreProcedimiento("Diagnòstic de la integració");
		ApiArchivoDigital apiArxiu = new ApiArchivoDigital(endpoint, cabecera);
		apiArxiu.setTrazas(false);
		String query = "(+TYPE:\"eni:documento\" AND @eni\\:csv:\"" + CODI_INEXISTENT + "\" -ASPECT:\"gdib:borrador\" -ASPECT:\"gdib:trasladado\") "
				+ " OR (+TYPE:\"gdib:documentoMigrado\" AND @gdib\\:hash:\"" + CODI_INEXISTENT + "\") ";
		SearchDocsResult result = apiArxiu.busquedaDocumentos(query, 0);
		if (result == null) {
			throw new IllegalStateException("L'API d'Arxiu Digital no ha retornat cap resposta");
		}
		RespuestaGenerica<ResParamSearchDocument> resposta = result.getSearchDocumentsResult();
		String code = resposta.getResult().getCode();
		if ("COD_000".equals(code) || "COD_001".equals(code)) {
			return "L'Arxiu ha respost correctament";
		}
		throw new IllegalStateException(code + " - " + resposta.getResult().getDescription());
	}

	/** Consulta d'un hash inexistent: el «no trobat» del servei antic és una resposta correcta. */
	private String provaCustodia() throws Exception {
		String endpoint = fitxer(PropertyConfig.PROP_OLD_SAVEKEEPING_ENDPOINT);
		try {
			Service service = new Service();
			Call call = (Call) service.createCall();
			call.setTargetEndpointAddress(new URL(endpoint));
			call.setOperationName(new QName(endpoint, "consultarCustodiaXMLPorHash"));
			// Com al servei: el timeout es pot canviar des del backoffice.
			Integer timeout = timeoutCustodia();
			if (timeout != null) {
				call.setTimeout(timeout);
			}
			call.invoke(new Object[] { CODI_INEXISTENT });
			return "La custòdia antiga ha respost correctament";
		} catch (Exception ex) {
			if (ex.getMessage() != null && ex.getMessage().contains(CUSTODIA_NO_TROBAT)) {
				return "La custòdia antiga ha respost correctament";
			}
			throw ex;
		}
	}

	/** Validació d'una signatura buida: qualsevol resposta del servei @firma és correcta. */
	private String provaValidacioFirmes() throws Exception {
		Properties propietats = configHelper.getProperties();
		IValidateSignaturePlugin plugin = new ValidateSignaturePlugin(
				PropertyConfig.PROP_BASE_PREFIX_VALIDATE_SIGNATURE,
				propietats);
		ValidateSignatureRequest request = new ValidateSignatureRequest();
		request.setLanguage("ca");
		request.setSignatureData("diagnostic".getBytes(StandardCharsets.UTF_8));
		ValidateSignatureResponse response = plugin.validateSignature(request);
		if (response == null) {
			throw new IllegalStateException("El sistema de validació de firma ha retornat una resposta <null>");
		}
		return "El sistema de validació de firmes ha respost correctament";
	}

	/** Conversió d'un text de pocs bytes a PDF: comprova que el convertidor funciona de punta a punta. */
	private String provaConversio() throws Exception {
		Properties propietats = configHelper.getFileProperties();
		IDocumentConverterPlugin convertidor = new OpenOfficeDocumentConverterPlugin(
				PropertyConfig.PROP_BASE_PREFIX_DOCUMENT_CONVERTER,
				propietats);
		ByteArrayOutputStream sortida = new ByteArrayOutputStream();
		convertidor.convertDocumentByMime(
				new ByteArrayInputStream("Diagnòstic de ConCSV".getBytes(StandardCharsets.UTF_8)),
				"text/plain",
				sortida,
				"application/pdf");
		if (sortida.size() == 0) {
			throw new IllegalStateException("El convertidor ha retornat un document buit");
		}
		return "El convertidor ha generat un PDF de " + sortida.size() + " bytes";
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

	private String fitxer(String key) {
		String value = configHelper.getEnvironmentProperty(key, null);
		if (value == null) {
			throw new IllegalStateException("La propietat " + key + " no està definida");
		}
		return value;
	}

	private MonitorIntegracioResource.DiagnosticResult error(String missatge, Throwable ex) {
		return new MonitorIntegracioResource.DiagnosticResult(false, null, missatge, ex != null ? traca(ex) : null);
	}

	/** Traça de l'excepció, sense la contrasenya de l'Arxiu i acotada. */
	private String traca(Throwable ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		return amagaContrasenya(writer.toString());
	}

	/**
	 * Text de l'error per mostrar: classe i missatge, sense la contrasenya de l'Arxiu si el
	 * missatge de la llibreria la inclou.
	 */
	private String missatge(Throwable ex) {
		return amagaContrasenya(ex.getClass().getSimpleName() + (ex.getMessage() != null ? ": " + ex.getMessage() : ""));
	}

	private String amagaContrasenya(String text) {
		String contrasenya = configHelper.getEnvironmentProperty(PropertyConfig.PROP_NEW_DIGITAL_ARCHIVE_PASSWORD, null);
		if (contrasenya != null && !contrasenya.isEmpty()) {
			text = text.replace(contrasenya, "***");
		}
		return text;
	}

}
