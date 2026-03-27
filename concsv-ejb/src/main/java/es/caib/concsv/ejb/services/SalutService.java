
package es.caib.concsv.ejb.services;

import es.caib.comanda.model.v1.salut.*;
import es.caib.comanda.ms.salut.helper.MonitorHelper;
import es.caib.concsv.ejb.annotation.ErrorInt;
import es.caib.concsv.ejb.annotation.PerformanceInt;
import es.caib.concsv.ejb.helpers.EstadisticaHelper;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import es.caib.concsv.ejb.helpers.SubsistemesHelper;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.facade.SalutServiceInterface;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ErrorInt
@PerformanceInt
@Stateless @Local({SalutServiceInterface.class})
public class SalutService implements SalutServiceInterface {

	@Inject private SubsistemesHelper subsistemesHelper;
	@Inject private IntegracionsHelper integracionsHelper;
    @Inject private EstadisticaHelper estadisticaHelper;

	private static final String CODE_APP = "CSV";
	private static final int MAX_CONNECTION_RETRY = 3;

	/** Mètode per al monotireig de l'aplicació **/
	@PermitAll
	public AppInfo appInfo(Map<String, Object> manifestInfo, HttpServletRequest request) throws GenericServiceException {
		return AppInfo.builder()
			.codi(CODE_APP)
			.nom("Concsv")
			.versio((String) manifestInfo.get("Implementation-Version"))
			.data(toOffsetDateTime(parseIsoDate(manifestInfo.get("Build-Timestamp"))))
			.revisio((String) manifestInfo.get("Implementation-SCM-Revision"))
			.jdkVersion((String) manifestInfo.get("Build-Jdk-Spec"))
			.integracions(getIntegracions())
			.subsistemes(getSubsistemes())
			.contexts(getContexts(getServerRoot(request)))
            .versioJboss(MonitorHelper.getApplicationServerInfo())
			.build();
	}

	/** Torna la llista d'integracions de l'aplicació **/
	private List<IntegracioInfo> getIntegracions() {
		return integracionsHelper.getIntegracionsInfo();
	}

	/** Torna la llista de subsistemes de l'aplicació **/
	private List<SubsistemaInfo> getSubsistemes() {
		return Arrays.stream(SubsistemesHelper.SubsistemesEnum.values())
			.map(subsistema -> SubsistemaInfo.builder().codi(subsistema.name()).nom(subsistema.getNom()).build())
			.collect(Collectors.toList());
	}

	private List<ContextInfo> getContexts(String baseUrl) {
		return List.of(
			ContextInfo.builder()
				.codi("FRONT")
				.nom("front")
				.path(baseUrl + "/concsvfront")
                .api(baseUrl + "/concsvfront/api")
				.build(),
			ContextInfo.builder()
				.codi("INT")
				.nom("API interna")
				.path(baseUrl + "/concsvapi/interna/swagger/")
                .api(baseUrl + "/concsvapi/interna")
				.build()
		);
	}

	/** Peticio per a obtenir la informacio de Salut **/
	@PermitAll
	public SalutInfo checkSalut(String versio, String performanceUrl) {
		var estatSalut = checkEstatSalut(performanceUrl);   // Estat
		var salutDatabase = checkDatabase();				// Base de dades (sols conté estadístiques)
		var integracions = checkIntegracions();			 	// Integracions

		SubsistemesHelper.SubsistemesInfo subsistemesInfo = subsistemesHelper.getSubsistemesInfo();
		var subsistemes = subsistemesInfo.getSubsistemesSalut();  // Subsistemes
		var estatGlobalSubsistemes = subsistemesInfo.getEstatGlobal();
		if (EstatSalutEnum.UP.equals(estatSalut.getEstat()) && !EstatSalutEnum.UP.equals(estatGlobalSubsistemes) && !EstatSalutEnum.UNKNOWN.equals(estatGlobalSubsistemes)) {
			estatSalut = EstatSalut.builder()
					.estat(estatGlobalSubsistemes)
					.latencia(estatSalut.getLatencia())
					.build();
		}

		return SalutInfo.builder()
				.codi(CODE_APP)
				.versio(versio)
				.data(OffsetDateTime.now())
				.estatGlobal(estatSalut)
				.estatBaseDeDades(salutDatabase)
				.integracions(integracions)
				.subsistemes(subsistemes)
//				.missatges(missatges) //No implementa un sistema de missatges
				.informacioSistema(MonitorHelper.getInfoSistema())
				.build();
	}

	private EstatSalut checkEstatSalut(String performanceUrl) {
		Instant start = Instant.now();
		EstatSalutEnum estat = EstatSalutEnum.UP;
		for (int i = 1; i <= MAX_CONNECTION_RETRY; i++) {
			try {
				Client client = ClientBuilder.newClient();
				client.target(performanceUrl).request().get(String.class);
				break;
			} catch (Exception e) {
				if (i == MAX_CONNECTION_RETRY) {
					estat = EstatSalutEnum.DOWN;
				}
			}
		}
		Integer latency = (int) Duration.between(start, Instant.now()).toMillis();
		return EstatSalut.builder()
			.estat(estat)
			.latencia(latency)
			.build();
	}

	/** Retornarem l'estat de la base de dades on es desa la informació d'estadístiques amb la seva latència. **/
    private EstatSalut checkDatabase() {
        Long latencyMs = estadisticaHelper.checkDatabaseLatency();
        EstatSalutEnum status = (latencyMs == null) ? EstatSalutEnum.DOWN : EstatSalutEnum.UP;
        Integer latency = (latencyMs == null) ? 0 :
                (latencyMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : latencyMs.intValue());
        return new EstatSalut(status, latency);
    }

	private List<IntegracioSalut> checkIntegracions() {
		return integracionsHelper.checkIntegracions();
	}

	private Date parseIsoDate(Object value) {
		if (value == null) { return null; }
		try {
			return Date.from(Instant.parse(value.toString()));
		} catch (Exception e) {
			log.warn("No se pudo parsear la fecha ISO: {}", value, e);
			return null;
		}
	}

	public static OffsetDateTime toOffsetDateTime(Date data) {
		return data != null ? data.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
	}

	private String getServerRoot(HttpServletRequest request) {
		if (request == null) { return ""; }
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if ((scheme.equals("http") && serverPort != 80) ||
			(scheme.equals("https") && serverPort != 443)) {
			url.append(":").append(serverPort);
		}
		return url.toString();
	}

}