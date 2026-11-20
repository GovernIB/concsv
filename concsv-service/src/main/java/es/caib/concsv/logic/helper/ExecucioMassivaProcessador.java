package es.caib.concsv.logic.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Procés en segon pla de les execucions massives: agafa la següent execució pendent i en tracta els
 * elements un per un. El crida {@code SchedulingConfig} cada cop que toca comprovar la cua.
 * <p>
 * Equivalent a {@code ExecucioMassivaServiceImpl.executeNextMassiveScheduledTask} de RIPEA. Aquesta
 * classe no és transaccional: totes les operacions amb la base de dades són a {@link ExecucioMassivaCuaHelper}
 * i {@link ExecucioMassivaHelper}, cadascuna amb la seva transacció, de manera que res no es perd si
 * el procés s'interromp a mig camí.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecucioMassivaProcessador {

	private final ExecucioMassivaCuaHelper cuaHelper;
	private final ExecucioMassivaHelper execucioMassivaHelper;

	/** Només una execució alhora per node: les crides que arribin mentre n'hi ha una en curs no fan res. */
	private final AtomicBoolean processant = new AtomicBoolean(false);
	private ScheduledExecutorService heartbeat;

	@PostConstruct
	void init() {
		heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
			Thread fil = new Thread(runnable, "concsv-massives-heartbeat");
			fil.setDaemon(true);
			return fil;
		});
	}

	@PreDestroy
	void destrueix() {
		if (heartbeat != null) {
			heartbeat.shutdownNow();
		}
	}

	/**
	 * Agafa la següent execució pendent (si n'hi ha) i la processa fins al final. L'execució es tanca
	 * sempre, encara que el procés falli.
	 */
	public void executeNextMassiveScheduledTask() {
		if (!processant.compareAndSet(false, true)) {
			log.debug("Ja hi ha una execució massiva en procés en aquest node");
			return;
		}
		try {
			Long execucioId = cuaHelper.reclamarPerProcessar();
			if (execucioId == null) {
				return;
			}
			processaIFinalitza(execucioId);
		} finally {
			processant.set(false);
		}
	}

	private void processaIFinalitza(Long execucioId) {
		ScheduledFuture<?> batec = heartbeat.scheduleWithFixedDelay(
				() -> renovaHeartbeat(execucioId),
				ExecucioMassivaCuaHelper.HEARTBEAT_INTERVAL_MS,
				ExecucioMassivaCuaHelper.HEARTBEAT_INTERVAL_MS,
				TimeUnit.MILLISECONDS);
		String error = null;
		try {
			processaComACreador(execucioId);
		} catch (Throwable th) {
			log.error("Error processant l'execució massiva {}", execucioId, th);
			error = th.getMessage() != null ? th.getMessage() : th.getClass().getName();
		} finally {
			batec.cancel(false);
		}
		// El tancament és el que treu l'execució de la cua: si falla, es reprendrà en el següent cicle
		// (els elements ja tractats no es repeteixen).
		try {
			execucioMassivaHelper.finalitza(execucioId, error);
		} catch (RuntimeException ex) {
			log.error("No s'ha pogut tancar l'execució massiva {}: queda oberta", execucioId, ex);
			return;
		}
		// TODO: Aquí s'hauria de cridar la funció per enviar el correu a l'usuari que ha creat l'execució (camp ENVIAR_CORREU)
		//  un cop tancada, si es decideix implementar-ho.
	}

	/**
	 * Tracta els elements amb l'usuari que va crear l'execució al context de seguretat, durant tota
	 * l'execució: l'auditoria de les dades modificades (qui les ha modificat) s'omple en el commit de la
	 * transacció de cada element, de manera que l'usuari s'ha de mantenir fins a la sortida de
	 * {@link ExecucioMassivaHelper#executaContingut}. En acabar es torna al que hi havia (l'usuari de sistema
	 * del planificador).
	 */
	void processaComACreador(Long execucioId) {
		Authentication original = SecurityContextHolder.getContext().getAuthentication();
		SecurityContextHolder.getContext().setAuthentication(
				ExecucioMassivaHelper.autenticacio(execucioMassivaHelper.creador(execucioId)));
		try {
			processa(execucioId);
		} finally {
			if (original != null) {
				SecurityContextHolder.getContext().setAuthentication(original);
			} else {
				SecurityContextHolder.clearContext();
			}
		}
	}

	/** Tracta els elements pendents en ordre, aturant-se si es demana cancel·lar. */
	void processa(Long execucioId) {
		List<Long> pendents = execucioMassivaHelper.idsPendents(execucioId);
		log.info("Processant l'execució massiva {} ({} elements pendents)", execucioId, pendents.size());
		for (Long contingutId : pendents) {
			// Es comprova abans de cada element: un element que ja ha començat no s'interromp, ja que
			// podria deixar la feina a mig fer.
			if (cuaHelper.isCancelacioSolicitada(execucioId)) {
				log.info("Cancel·lada l'execució massiva {}: s'atura abans de l'element {}", execucioId, contingutId);
				break;
			}
			try {
				execucioMassivaHelper.executaContingut(contingutId);
			} catch (RuntimeException ex) {
				// No s'ha pogut ni desar el resultat de l'element: se'n deixa constància a part.
				log.error("No s'ha pogut tractar l'element {} de l'execució massiva {}", contingutId, execucioId, ex);
				try {
					execucioMassivaHelper.marcaContingutError(contingutId, ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
				} catch (RuntimeException ex2) {
					log.error("Tampoc s'ha pogut marcar com a error l'element {}", contingutId, ex2);
				}
			}
		}
	}

	private void renovaHeartbeat(Long execucioId) {
		try {
			cuaHelper.actualitzarHeartbeat(execucioId);
		} catch (RuntimeException ex) {
			log.warn("No s'ha pogut renovar el batec de l'execució massiva {}", execucioId, ex);
		}
	}

}
