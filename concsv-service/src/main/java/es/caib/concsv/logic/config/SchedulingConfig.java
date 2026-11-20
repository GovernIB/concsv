package es.caib.concsv.logic.config;

import es.caib.concsv.logic.helper.ExecucioMassivaHelper;
import es.caib.concsv.logic.helper.ExecucioMassivaProcessador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Tasques en segon pla del backoffice. Ara només una: comprovar cada cert temps si hi ha execucions
 * massives pendents i processar-les (equivalent al {@code SchedulingConfig} de RIPEA).
 * <p>
 * Viu a la capa de negoci perquè la carregui tant el context Spring de l'EJB (JBoss) com el del mode
 * Boot, que no té EJBs ni, per tant, temporitzadors d'EJB.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig implements SchedulingConfigurer {

	/** Periode de comprovació de les execucions massives pendents: 1 minut (fix, no és configurable). */
	static final long PERIODE_COMPROVACIO_MS = 60_000L;
	/** Espera abans de la primera comprovació, perquè l'aplicació acabi d'arrencar. */
	static final long RETARD_INICIAL_MS = 30_000L;
	/** Usuari amb què s'executa el procés (l'auditoria en necessita un). */
	static final String USUARI_SISTEMA = "SYSTEM_CONCSV";

	private final ExecucioMassivaProcessador execucioMassivaProcessador;

	@Override
	public void configureTasks(ScheduledTaskRegistrar registrar) {
		registrar.addTriggerTask(this::executaExecucionsMassives, triggerContext -> {
			PeriodicTrigger trigger = new PeriodicTrigger(PERIODE_COMPROVACIO_MS, TimeUnit.MILLISECONDS);
			// L'interval es mesura des del final de l'execució anterior: no es comprova la cua
			// mentre n'hi ha una en procés, que si no es processarien repetides.
			trigger.setFixedRate(false);
			// Només compta la primera vegada (quan encara no hi ha cap execució anterior).
			trigger.setInitialDelay(RETARD_INICIAL_MS);
			Date properaExecucio = trigger.nextExecutionTime(triggerContext);
			log.debug("Propera comprovació de les execucions massives: {}", properaExecucio);
			return properaExecucio;
		});
	}

	private void executaExecucionsMassives() {
		SecurityContextHolder.getContext().setAuthentication(ExecucioMassivaHelper.autenticacio(USUARI_SISTEMA));
		try {
			execucioMassivaProcessador.executeNextMassiveScheduledTask();
		} catch (Throwable th) {
			// Una excepció no ha d'arribar al planificador: cancel·laria la tasca per sempre.
			log.error("Error en la comprovació de les execucions massives", th);
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

}
