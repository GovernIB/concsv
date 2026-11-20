package es.caib.concsv.logic.config;

import es.caib.concsv.logic.helper.ExecucioMassivaProcessador;
import org.junit.Test;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.config.TriggerTask;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Proves de {@link SchedulingConfig}.
 */
public class SchedulingConfigTest {

	@Test
	public void comprovaLaCuaCadaMinut() {
		assertEquals(60_000L, SchedulingConfig.PERIODE_COMPROVACIO_MS);
	}

	@Test
	public void registraUnaTascaAmbEsperaInicialIPeriodeFix() {
		ExecucioMassivaProcessador processador = mock(ExecucioMassivaProcessador.class);
		ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();

		new SchedulingConfig(processador).configureTasks(registrar);

		assertEquals(1, registrar.getTriggerTaskList().size());
		TriggerTask tasca = registrar.getTriggerTaskList().get(0);
		// Primera execució: sense cap execució anterior s'espera el retard inicial
		long abans = System.currentTimeMillis();
		Date primera = tasca.getTrigger().nextExecutionTime(new org.springframework.scheduling.support.SimpleTriggerContext());
		assertTrue(primera.getTime() >= abans + SchedulingConfig.RETARD_INICIAL_MS - 1000);

		// Les següents, des del final de l'anterior, un periode després
		Date final1 = new Date(abans);
		Date segona = tasca.getTrigger().nextExecutionTime(
				new org.springframework.scheduling.support.SimpleTriggerContext(final1, final1, final1));
		assertEquals(abans + SchedulingConfig.PERIODE_COMPROVACIO_MS, segona.getTime());
	}

	@Test
	public void laTascaCridaElProcessadorIAcabaSenseAutenticacio() {
		ExecucioMassivaProcessador processador = mock(ExecucioMassivaProcessador.class);
		ScheduledTaskRegistrar registrar = new ScheduledTaskRegistrar();
		new SchedulingConfig(processador).configureTasks(registrar);

		registrar.getTriggerTaskList().get(0).getRunnable().run();

		verify(processador).executeNextMassiveScheduledTask();
		assertEquals(null, org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication());
	}

}
