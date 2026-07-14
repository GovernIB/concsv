package es.caib.concsv.logic.helper;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import es.caib.comanda.model.server.monitoring.IntegracioInfo;
import es.caib.comanda.model.server.monitoring.IntegracioSalut;
import es.caib.comanda.ms.salut.helper.IntegracioApp;
import es.caib.comanda.ms.salut.helper.SalutComponentsHelper;
import es.caib.comanda.ms.salut.helper.SalutComponentsHelper.InformeSalutComponents;
import es.caib.comanda.ms.salut.helper.components.MonitorComponentsMemoria;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class IntegracionsHelper {

	/** Instància del helper per obtenir els informes de les integracions. */
	private SalutComponentsHelper subsistemes = null;
	/** Instància pel monitor d'integracions. */
	private MonitorComponentsMemoria monitor = null;

	@Getter
	private final List<IntegracioApp> integracions = List.of(
		IntegracioApp.ARX,
		IntegracioApp.SIG,
		IntegracioApp.VFI
	);

	@PostConstruct
	public void init() {
		initializeMetrics();
	}

	private void initializeMetrics() {
		// Core per estadístiques i fallback
		this.monitor = new MonitorComponentsMemoria(20);
		// Totes les integracions són crítiques
		Function<String, Boolean> esCritic = componentId -> true;
		// Servei que genera l'informe complet
		this.subsistemes = new SalutComponentsHelper(monitor, esCritic);
	}

	/**
	 * Registra una operació, indicant la durada i si és un error. En cas d'enviar null isError no registrarà cap operació.
	 **/
	public void addOperation(IntegracioApp app, long startTime, Boolean isError) {
		if (isError == null) return;
		if (isError) {
			addErrorOperation(app);
		} else {
			long durada = System.currentTimeMillis() - startTime;
			addSuccessOperation(app, durada);
		}
	}

	/**
	 * Registra una operació correcta amb durada en ms (global + local)
	 **/
	public void addSuccessOperation(IntegracioApp app, long duradaMs) {
		this.monitor.registraExit(app.getCodi(), duradaMs);
	}

	/**
	 * Registra una operació errònia (global + local)
	 **/
	public void addErrorOperation(IntegracioApp app) {
		this.monitor.registraError(app.getCodi());
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
