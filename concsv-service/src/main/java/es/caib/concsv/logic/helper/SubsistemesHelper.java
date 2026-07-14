package es.caib.concsv.logic.helper;

import java.util.List;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import es.caib.comanda.model.server.monitoring.EstatSalutEnum;
import es.caib.comanda.model.server.monitoring.SubsistemaSalut;
import es.caib.comanda.ms.salut.helper.SalutComponentsHelper;
import es.caib.comanda.ms.salut.helper.SalutComponentsHelper.InformeSalutComponents;
import es.caib.comanda.ms.salut.helper.components.MonitorComponentsMemoria;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class SubsistemesHelper {

	/** Instància del helper per obtenir els informes de subsistemes. */
	private SalutComponentsHelper subsistemes = null;
	/** Instància pel monitor de subsistemes. */
	private MonitorComponentsMemoria monitor = null;

	@PostConstruct
	public void init() {
		initializeMetrics();
	}

	private void initializeMetrics() {
		// Core per estadístiques i fallback
		this.monitor = new MonitorComponentsMemoria(20);
		// Determina quins components són crítics
		Function<String, Boolean> esCritic = componentId -> 
												SubsistemesEnum.valueOf(componentId).isSistemaCritic();
		// Servei que genera l'informe complet
		this.subsistemes = new SalutComponentsHelper(monitor, esCritic);
	}

	@Getter
	public enum SubsistemesEnum {
		EXC("Documents exclosos", false),
		CHE("CheckHash", false),
		MET("Obtenir metadades", false),
		ORI("Obtenir document original", true),
		IMP("Obtenir document imprimible", true),
		ENI("Obtenir ENI document", false);
		private final String nom;
		private final boolean sistemaCritic;
		SubsistemesEnum(String nom, boolean sistemaCritic) {
			this.nom = nom;
			this.sistemaCritic = sistemaCritic;
		}
	}

	/** Registra una operació, indicant la durada i si és un error. En cas d'enviar null isError no registrarà cap operació. **/
	public void addOperation(SubsistemesEnum subsistema, long startTime, Boolean isError) {
		if (isError == null) return;
		if (isError) {
			addErrorOperation(subsistema);
		} else {
			long durada = System.currentTimeMillis() - startTime;
			addSuccessOperation(subsistema, durada);
		}
	}

	/** Registra una operació correcta amb durada en ms (global + local) **/
	public void addSuccessOperation(SubsistemesEnum subsistema, long duracio) {
		this.monitor.registraExit(subsistema.name(), duracio);
	}

	/** Registra una operació errònia (global + local) **/
	public void addErrorOperation(SubsistemesEnum subsistema) {
		this.monitor.registraError(subsistema.name());
	}

	/** Retorna informació de salut bàsica dels subsistemes **/
	public SubsistemesInfo getSubsistemesInfo() {
		InformeSalutComponents informeSubsistemes = this.subsistemes.obtenInforme();
		return SubsistemesInfo.builder()
			.subsistemesSalut(informeSubsistemes.toSubsistemesSalut())
			.estatGlobal(informeSubsistemes.getEstatGlobal())
			.build();
	}

	@Getter
	@Builder
	public static class SubsistemesInfo {
		private final List<SubsistemaSalut> subsistemesSalut;
		private final EstatSalutEnum estatGlobal;
	}

}
