package es.caib.concsv.logic.service;

import es.caib.comanda.model.server.monitoring.*;
import es.caib.comanda.ms.log.helper.LogHelper;
import es.caib.comanda.ms.salut.helper.MonitorHelper;
import es.caib.concsv.logic.annotation.ErrorInt;
import es.caib.concsv.logic.annotation.PerformanceInt;
import es.caib.concsv.logic.estadistiques.DimensioEnum;
import es.caib.concsv.logic.estadistiques.FetEnum;
import es.caib.concsv.logic.helper.EstadisticaHelper;
import es.caib.concsv.logic.helper.IntegracionsHelper;
import es.caib.concsv.logic.helper.SubsistemesHelper;
import es.caib.concsv.logic.intf.enums.ResultTypeEnum;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import es.caib.concsv.logic.intf.model.EnviamentOrigen;
import es.caib.concsv.logic.intf.model.EnviamentTipus;
import es.caib.concsv.logic.intf.qualifier.LogicService;
import es.caib.concsv.logic.intf.service.ComandaServiceInterface;
import es.caib.concsv.persistence.entity.ExplotDimensioEntity;
import es.caib.concsv.persistence.entity.ExplotFetsEntity;
import es.caib.concsv.persistence.entity.ExplotTempsEntity;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ErrorInt
@PerformanceInt
@LogicService
@ApplicationScoped
public class ComandaService implements ComandaServiceInterface {

	@Inject
	private EstadisticaHelper estadisticaHelper;
	@Inject
	private SubsistemesHelper subsistemesHelper;
	@Inject
	private IntegracionsHelper integracionsHelper;
	@Inject
	@ConfigProperty(name = "es.caib.concsv.logs.location", defaultValue = "standalone/log/")
	private String LOGS_LOCATION;

	private static final String CODE_APP = "CSV";
	private static final int MAX_CONNECTION_RETRY = 3;

	@PostConstruct
	public void init() {
	}

	/**
	 * Mètode per al monotireig de l'aplicació
	 **/
	@PermitAll
	public AppInfo appInfo(Map<String, Object> manifestInfo, String serverRoot) throws GenericServiceException {
		return new AppInfo().
			codi(CODE_APP).
			nom("Concsv").
			versio((String) manifestInfo.get("Implementation-Version")).
			data(toOffsetDateTime(parseIsoDate(manifestInfo.get("Build-Timestamp")))).
			revisio((String) manifestInfo.get("Implementation-SCM-Revision")).
			jdkVersion((String) manifestInfo.get("Build-Jdk-Spec")).
			integracions(getIntegracions()).
			subsistemes(getSubsistemes()).
			contexts(getContexts(serverRoot)).
			versioJboss(MonitorHelper.getApplicationServerInfo());
	}

	/**
	 * Peticio per a obtenir la informacio de Salut
	 **/
	@PermitAll
	public SalutInfo checkSalut(String versio, String performanceUrl) {
		EstatSalut estatSalut = checkEstatSalut(performanceUrl);
		EstatSalut salutDatabase = checkDatabase();
		List<IntegracioSalut> integracions = checkIntegracions();
		SubsistemesHelper.SubsistemesInfo subsistemesInfo = subsistemesHelper.getSubsistemesInfo();
		List<SubsistemaSalut> subsistemes = subsistemesInfo.getSubsistemesSalut();
		EstatSalutEnum estatGlobalSubsistemes = subsistemesInfo.getEstatGlobal();
		if (EstatSalutEnum.UP.equals(estatSalut.getEstat()) && !EstatSalutEnum.UP.equals(estatGlobalSubsistemes) && !EstatSalutEnum.UNKNOWN.equals(estatGlobalSubsistemes)) {
			estatSalut = new EstatSalut().
				estat(estatGlobalSubsistemes).
				latencia(estatSalut.getLatencia());
		}
		return new SalutInfo().
			codi(CODE_APP).
			versio(versio).
			data(OffsetDateTime.now()).
			estatGlobal(estatSalut).
			estatBaseDeDades(salutDatabase).
			integracions(integracions).
			subsistemes(subsistemes).
			// missatges(missatges). // No implementa un sistema de missatges
				informacioSistema(MonitorHelper.getInfoSistema());
	}

	/**
	 * Registra una petició en memòria (OK/INVALID/ERROR)
	 */
	@Override
	@PermitAll
	public void registrarPeticio(String origen, ResultTypeEnum result, long durationMillis) {
		estadisticaHelper.register(origen, result, durationMillis);
	}

	/**
	 * Persisteix les dades acumulades a la base de dades
	 */
	@Override
	@PermitAll
	public void flushEstadisticas(LocalDate date) {
		estadisticaHelper.flushToDatabase(date);
	}

	/**
	 * Esborra tots els Temps i Fets anteriors a la data límit
	 */
	@Override
	@PermitAll
	public void netejarEstadistiques(LocalDate date) {
		estadisticaHelper.netejarEstadistiques(date);
	}

	@Override
	@PermitAll
	public RegistresEstadistics consultaUltimesEstadistiques() {
		return consultaEstadistiques(LocalDate.now().minusDays(1));
	}

	@Override
	@PermitAll
	public RegistresEstadistics consultaEstadistiques(LocalDate data) {
		return getRegistresEstadistics(data);
	}

	@Override
	@PermitAll
	public List<RegistresEstadistics> consultaEstadistiques(LocalDate dataInici, LocalDate dataFi) {
		List<RegistresEstadistics> result = new ArrayList<>();
		LocalDate currentDate = dataInici;
		while (!currentDate.isAfter(dataFi)) {
			result.add(getRegistresEstadistics(currentDate));
			currentDate = currentDate.plusDays(1);
		}
		return result;
	}

	@Override
	@PermitAll
	public List<DimensioDesc> getDimensions() {
		List<String> tipus = Arrays.stream(EnviamentTipus.values()).map(Enum::name).sorted().collect(Collectors.toList());
		List<String> origens = Arrays.stream(EnviamentOrigen.values()).map(Enum::name).sorted().collect(Collectors.toList());
		return List.of(
			new DimensioDesc().
				codi(DimensioEnum.TIP.name()).
				nom(DimensioEnum.TIP.getNom()).
				descripcio(DimensioEnum.TIP.getDescripcio()).
				valors(tipus),
			new DimensioDesc().
				codi(DimensioEnum.ORI.name()).
				nom(DimensioEnum.ORI.getNom()).
				descripcio(DimensioEnum.ORI.getDescripcio()).
				valors(origens));
	}

	@Override
	@PermitAll
	public List<IndicadorDesc> getIndicadors() {
		return Arrays.stream(FetEnum.values()).
			map(fet -> new IndicadorDesc().
				codi(fet.name()).
				nom(fet.getNom()).
				descripcio(fet.getDescripcio()).
				format(Format.LONG)).
			collect(Collectors.toList());
	}

	@Override
	public FitxerContingut getFitxerByNom(String nomFitxer) throws GenericServiceException {
		return LogHelper.getFitxerByNom(LOGS_LOCATION, nomFitxer);
	}

	@Override
	public List<String> llegitUltimesLinies(String nomFitxer, Long nLinies) {
		return LogHelper.readLastNLines(LOGS_LOCATION, nomFitxer, nLinies);
	}

	@Override
	public List<FitxerInfo> llistarFitxers() throws GenericServiceException {
		return LogHelper.llistarFitxers(LOGS_LOCATION, "concsv");
	}

	/**
	 * Torna la llista d'integracions de l'aplicació
	 **/
	private List<IntegracioInfo> getIntegracions() {
		return integracionsHelper.getIntegracionsInfo();
	}

	/**
	 * Torna la llista de subsistemes de l'aplicació
	 **/
	private List<SubsistemaInfo> getSubsistemes() {
		return Arrays.stream(SubsistemesHelper.SubsistemesEnum.values())
			.map(subsistema -> new SubsistemaInfo().codi(subsistema.name()).nom(subsistema.getNom()))
			.collect(Collectors.toList());
	}

	private List<ContextInfo> getContexts(String baseUrl) {
		return List.of(
			new ContextInfo().
				codi("FRONT").
				nom("front").
				path(baseUrl + "/concsvfront").
				api(baseUrl + "/concsvfront/api"),
			new ContextInfo().
				codi("INT").
				nom("API interna").
				path(baseUrl + "/concsvapi/interna/swagger/").
				api(baseUrl + "/concsvapi/interna"));
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
		return new EstatSalut().
			estat(estat).
			latencia(latency);
	}

	/**
	 * Retornarem l'estat de la base de dades on es desa la informació d'estadístiques amb la seva latència.
	 **/
	private EstatSalut checkDatabase() {
		Long latencyMs = 1L; // TODO estadisticaHelper.checkDatabaseLatency();
		EstatSalutEnum status = (latencyMs == null) ? EstatSalutEnum.DOWN : EstatSalutEnum.UP;
		Integer latency = (latencyMs == null) ? 0 :
			(latencyMs > Integer.MAX_VALUE ? Integer.MAX_VALUE : latencyMs.intValue());
		return new EstatSalut().estat(status).latencia(latency);
	}

	private List<IntegracioSalut> checkIntegracions() {
		return integracionsHelper.checkIntegracions();
	}

	private Date parseIsoDate(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return Date.from(Instant.parse(value.toString()));
		} catch (Exception e) {
			log.warn("No se pudo parsear la fecha ISO: {}", value, e);
			return null;
		}
	}

	private static OffsetDateTime toOffsetDateTime(Date data) {
		return data != null ? data.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime() : null;
	}

	private RegistresEstadistics getRegistresEstadistics(LocalDate data) {
		ExplotTempsEntity temps = estadisticaHelper.findFirstTempsByData(data).orElse(null);
		if (temps == null) {
			return new RegistresEstadistics().
				temps(data.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()).
				fets(List.of());
		} else {
			List<ExplotFetsEntity> fets = estadisticaHelper.findFetByTemps(temps);
			return new RegistresEstadistics().
				temps(data.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime()).
				fets(fets.stream().map(this::toRegistreEstadistic).collect(Collectors.toList()));
		}
	}

	private RegistreEstadistic toRegistreEstadistic(ExplotFetsEntity fet) {
		return new RegistreEstadistic().
			dimensions(toDimensions(fet.getDimensio())).
			fets(toFets(fet));
	}

	private List<Dimensio> toDimensions(ExplotDimensioEntity dimensio) {
		return List.of(
			new Dimensio().
				codi(DimensioEnum.TIP.name()).
				valor(dimensio.getTipus() != null ? dimensio.getTipus().name() : null),
			new Dimensio().
				codi(DimensioEnum.ORI.name()).
				valor(dimensio.getOrigen() != null ? dimensio.getOrigen().name() : null));
	}

	private List<Fet> toFets(ExplotFetsEntity fet) {
		return List.of(
			new Fet().
				codi(FetEnum.CORRECTE.name()).
				valor(fet.getCorrecte() != null ? fet.getCorrecte().doubleValue() : null),
			new Fet().
				codi(FetEnum.CODI_INVALID.name()).
				valor(fet.getCodiInvalid() != null ? fet.getCodiInvalid().doubleValue() : null),
			new Fet().
				codi(FetEnum.ERROR.name()).
				valor(fet.getError() != null ? fet.getError().doubleValue() : null),
			new Fet().
				codi(FetEnum.TEMPS_MITJ_CORRECTE.name()).
				valor(fet.getTempsMigCorrecte() != null ? fet.getTempsMigCorrecte().doubleValue() : null));
	}

}
