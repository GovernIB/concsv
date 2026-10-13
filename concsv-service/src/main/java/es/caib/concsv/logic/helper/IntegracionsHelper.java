package es.caib.concsv.logic.helper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import es.caib.comanda.model.server.monitoring.IntegracioInfo;
import es.caib.comanda.model.server.monitoring.IntegracioSalut;
import es.caib.comanda.ms.salut.helper.IntegracioApp;
import es.caib.comanda.ms.salut.helper.SalutComponentsHelper;
import es.caib.comanda.ms.salut.helper.SalutComponentsHelper.InformeSalutComponents;
import es.caib.comanda.ms.salut.helper.components.MonitorComponentsMemoria;
import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.logic.intf.model.IntegracioAccioTipusEnum;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class IntegracionsHelper {

	/** Instància del helper per obtenir els informes de les integracions. */
	private SalutComponentsHelper subsistemes = null;
	/** Instància pel monitor d'integracions. */
	private MonitorComponentsMemoria monitor = null;
	/** Escriptor del monitor d'integracions del backoffice (null als tests unitaris). */
	@Inject
	private MonitorIntegracioHelper monitorIntegracioHelper;

	@Getter
	private final List<IntegracioApp> integracions = List.of(
		IntegracioApp.ARX,
		IntegracioApp.CUS,
		IntegracioApp.VFI,
		IntegracioApp.CDO
	);

	@PostConstruct
	public void init() {
		initializeMetrics();
	}

	private void initializeMetrics() {
		// Core per estadístiques i fallback
		this.monitor = new MonitorComponentsMemoria(20);
		// Llistat de codis d'integració.
		List<String> codisIntegracions = new ArrayList<String>();
		for (IntegracioApp integracio : integracions ) {
			codisIntegracions.add(integracio.getCodi());
		}
		// Totes les integracions són crítiques
		Function<String, Boolean> esCritic = componentId -> true;
		// Servei que genera l'informe complet
		this.subsistemes = new SalutComponentsHelper(monitor, esCritic, codisIntegracions);
	}

	/**
	 * Registra una petició a una integració: per a Comanda (comptadors en memòria) i per al monitor
	 * d'integracions del backoffice (una fila a la taula, escrita en segon pla).
	 * <p>
	 * Si arriba una excepció la petició és un error; si no, és correcta. Qui crida decideix si una
	 * excepció és d'integració: per no registrar res (p. ex. quan no s'ha fet cap crida remota)
	 * simplement no s'ha de cridar aquest mètode.
	 *
	 * @param app integració cridada.
	 * @param startTime instant d'inici de la crida (ms).
	 * @param descripcio què s'ha fet (p. ex. 'Cerca de document per CSV').
	 * @param endpoint adreça de la integració; pot ser null.
	 * @param parametres paràmetres de la crida (p. ex. csv, uuid); pot ser null. Mai credencials ni
	 *        el contingut dels documents.
	 * @param ex excepció de la crida, o null si ha anat bé.
	 **/
	public void addOperation(IntegracioApp app, long startTime, String descripcio, String endpoint,
			Map<String, String> parametres, Throwable ex) {
		long durada = System.currentTimeMillis() - startTime;
		if (ex == null) {
			addSuccessOperation(app, durada, descripcio, endpoint, parametres);
		} else {
			addErrorOperation(app, durada, descripcio, endpoint, parametres, ex);
		}
	}

	/**
	 * Registra una operació correcta amb durada en ms (global + local) i la desa al monitor.
	 **/
	public void addSuccessOperation(IntegracioApp app, long duradaMs, String descripcio, String endpoint,
			Map<String, String> parametres) {
		this.monitor.registraExit(app.getCodi(), duradaMs);
		encua(MonitorIntegracioHelper.Registre.builder().
				data(new Date()).
				codi(codiMonitor(app)).
				descripcio(descripcio).
				endpoint(endpoint).
				parametres(parametres).
				tipus(IntegracioAccioTipusEnum.ENVIAMENT).
				tempsResposta(duradaMs).
				estat(IntegracioAccioEstatEnum.OK).
				build());
	}

	/**
	 * Registra una operació errònia (global + local) i la desa al monitor amb l'excepció i la seva traça.
	 **/
	public void addErrorOperation(IntegracioApp app, long duradaMs, String descripcio, String endpoint,
			Map<String, String> parametres, Throwable ex) {
		this.monitor.registraError(app.getCodi());
		String causa = ex != null ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : null;
		encua(MonitorIntegracioHelper.Registre.builder().
				data(new Date()).
				codi(codiMonitor(app)).
				descripcio(descripcio).
				endpoint(endpoint).
				parametres(parametres).
				tipus(IntegracioAccioTipusEnum.ENVIAMENT).
				tempsResposta(duradaMs).
				estat(IntegracioAccioEstatEnum.ERROR).
				errorDescripcio("Error en: " + (descripcio != null ? descripcio : app.getNom())).
				excepcioMessage(causa).
				excepcioStacktrace(ex != null ? stacktrace(ex) : null).
				build());
	}

	/**
	 * Construeix el mapa de paràmetres d'una crida a partir de parelles clau, valor. Les parelles
	 * amb valor null s'ometen.
	 **/
	public static Map<String, String> params(String... clauValor) {
		Map<String, String> params = new LinkedHashMap<>();
		for (int i = 0; i + 1 < clauValor.length; i += 2) {
			if (clauValor[i] != null && clauValor[i + 1] != null) {
				params.put(clauValor[i], clauValor[i + 1]);
			}
		}
		return params;
	}

	/** Passa la petició al monitor sense deixar mai que una fallada arribi a qui ha fet la crida. */
	private void encua(MonitorIntegracioHelper.Registre registre) {
		try {
			if (monitorIntegracioHelper != null && registre.getCodi() != null) {
				monitorIntegracioHelper.encua(registre);
			}
		} catch (Exception ex) {
			log.debug("No s'ha pogut encuar la petició al monitor d'integracions: {}", ex.toString());
		}
	}

	/** Codi del monitor de cada integració de Comanda; null si no es mostra al monitor. */
	static IntegracioCodiEnum codiMonitor(IntegracioApp app) {
		switch (app) {
		case ARX:
			return IntegracioCodiEnum.ARXIU;
		case CUS:
			return IntegracioCodiEnum.CUSTODIA;
		case VFI:
			return IntegracioCodiEnum.VALIDACIO_FIRMES;
		case CDO:
			return IntegracioCodiEnum.CONVERSIO;
		default:
			return null;
		}
	}

	private static String stacktrace(Throwable ex) {
		StringWriter writer = new StringWriter();
		ex.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

	/** Només per a tests. */
	void setMonitorIntegracioHelper(MonitorIntegracioHelper monitorIntegracioHelper) {
		this.monitorIntegracioHelper = monitorIntegracioHelper;
	}

	/**
	 * Retorna informacio de les integracions
	 **/
	public List<IntegracioInfo> getIntegracionsInfo() {
		return integracions.stream()
			.map(app -> new IntegracioInfo().codi(app.getCodi()).nom(app.getNom()))
			.collect(Collectors.toList());
	}

	/**
	 * Retorna l'estat de peticions d'integracions (basat en l'última finestra local)
	 **/
	public List<IntegracioSalut> checkIntegracions() {
		InformeSalutComponents informeSubsistemes = this.subsistemes.obtenInforme();
		return informeSubsistemes.toIntegracionsSalut();
	}
}
