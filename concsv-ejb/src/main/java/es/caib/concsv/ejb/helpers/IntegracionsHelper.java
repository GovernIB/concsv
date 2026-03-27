package es.caib.concsv.ejb.helpers;

import es.caib.comanda.model.v1.salut.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
public class IntegracionsHelper {

	@Inject @Setter private MeterRegistry registry; // Global
	private MeterRegistry localRegistry; // Local
	private final Map<IntegracioApp, Metrics> metrics = new ConcurrentHashMap<>();

	@Getter
	private final List<IntegracioApp> integracions = List.of(
		IntegracioApp.ARX,
		IntegracioApp.SIG,
		IntegracioApp.TIN,
		IntegracioApp.VFI
	);

	private static class Metrics {
		Timer timerOkGlobal;
		Counter counterErrorGlobal;
		Timer timerOkLocal;
		Counter counterErrorLocal;
	}

	@PostConstruct
	public void init() {
		if (registry == null) {
			log.warn("MeterRegistry no inicialitzat. No es registraran mètriques d’integració.");
			return;
		}
		initializeMetrics();
	}

	private void initializeMetrics() {
		// Reset local registry
		if (localRegistry != null) {
			try {
				localRegistry.close();
			} catch (Exception ignore) {}
		}
		localRegistry = new SimpleMeterRegistry();

		for (IntegracioApp app : IntegracioApp.values()) {
			Metrics m = metrics.computeIfAbsent(app, k -> new Metrics());

			// Global timers/counters
			if (registry != null && m.timerOkGlobal == null) {
				m.timerOkGlobal = Timer.builder("integracio." + app.name().toLowerCase())
					.tags("result", "success")
					.publishPercentiles(0.5, 0.75, 0.95, 0.99)
					.publishPercentileHistogram()
					.register(registry);

				m.counterErrorGlobal = Counter.builder("integracio." + app.name().toLowerCase() + ".errors")
					.register(registry);
			}

			// Local timers/counters
			m.timerOkLocal = Timer.builder("integracio." + app.name().toLowerCase() + ".local")
				.tags("result", "success")
				.publishPercentiles(0.5, 0.75, 0.95, 0.99)
				.publishPercentileHistogram()
				.register(localRegistry);

			m.counterErrorLocal = Counter.builder("integracio." + app.name().toLowerCase() + ".local.errors")
				.register(localRegistry);
		}
	}

	private void resetLocalMetrics() {
		initializeMetrics();
	}

	/** Registra una operació, indicant la durada i si és un error. En cas d'enviar null isError no registrarà cap operació. **/
	public void addOperation(IntegracioApp app, long startTime, Boolean isError) {
		if (isError == null) return;
		if (isError) {
			addErrorOperation(app);
		} else {
			long durada = System.currentTimeMillis() - startTime;
			addSuccessOperation(app, durada);
		}
	}

	/** Registra una operació correcta amb durada en ms (global + local) **/
	public void addSuccessOperation(IntegracioApp app, long duradaMs) {
		Metrics m = metrics.computeIfAbsent(app, k -> new Metrics());

		if (m.timerOkGlobal != null) {
			m.timerOkGlobal.record(duradaMs, TimeUnit.MILLISECONDS);
		}
		if (m.timerOkLocal != null) {
			m.timerOkLocal.record(duradaMs, TimeUnit.MILLISECONDS);
		}
	}

	/** Registra una operació errònia (global + local) **/
	public void addErrorOperation(IntegracioApp app) {
		Metrics m = metrics.computeIfAbsent(app, k -> new Metrics());

		if (m.counterErrorGlobal != null) {
			m.counterErrorGlobal.increment();
		}
		if (m.counterErrorLocal != null) {
			m.counterErrorLocal.increment();
		}
	}

	/** Retorna informació de salut bàsica d'una integració **/
	private EstatSalut getEstat(IntegracioApp app) {
		Metrics m = metrics.computeIfAbsent(app, k -> new Metrics());

		Integer latenciaMitja = (m.timerOkLocal != null && m.timerOkLocal.count() > 0)
			? (int) m.timerOkLocal.mean(TimeUnit.MILLISECONDS)
			: null;

		Long totalOk = (m.timerOkLocal != null) ? m.timerOkLocal.count() : 0L;
		Long totalError = (m.counterErrorLocal != null) ? (long) m.counterErrorLocal.count() : 0L;

		EstatSalutEnum estat = calculaEstat(totalOk, totalError);

		return EstatSalut.builder()
			.latencia(latenciaMitja)
			.estat(estat)
			.build();
	}

	private EstatSalutEnum calculaEstat(Long totalOk, Long totalError) {
		long ok = totalOk != null ? totalOk : 0L;
		long err = totalError != null ? totalError : 0L;
		long total = ok + err;

		if (total == 0) return EstatSalutEnum.UNKNOWN;

		int errorPct = (int) Math.round(err * 100.0 / total);

		if (errorPct >= 100) return EstatSalutEnum.DOWN;
		if (errorPct > 30) return EstatSalutEnum.ERROR;
		if (errorPct > 10) return EstatSalutEnum.DEGRADED;
		if (errorPct < 5) return EstatSalutEnum.UP;
		return EstatSalutEnum.WARN;
	}

	/** Retorna informacio de les integracions **/
	public List<IntegracioInfo> getIntegracionsInfo() {
		return integracions.stream()
			.map(app -> IntegracioInfo.builder()
				.integracioApp(app)
				.build())
			.collect(Collectors.toList());
	}

	/** Retorna l'estat de peticions d'integracions (basat en l'última finestra local) **/
	public List<IntegracioSalut> checkIntegracions() {
		List<IntegracioSalut> result = new ArrayList<>();
		for (IntegracioApp app : integracions) {
			EstatSalut estat = getEstat(app);
			IntegracioSalut integracioSalut = IntegracioSalut.builder()
				.codi(formatIntegracioCodi(app))
				.estat(estat.getEstat())
				.latencia(estat.getLatencia())
				.peticions(getPeticions(app))
				.build();

			result.add(integracioSalut);
		}

		resetLocalMetrics();
		return result;
	}

	private IntegracioPeticions getPeticions(IntegracioApp app) {
		Metrics m = metrics.computeIfAbsent(app, k -> new Metrics());

        Long totalOk = m.timerOkGlobal != null ? m.timerOkGlobal.count() : 0L;
        Long totalError = m.counterErrorGlobal != null ? (long) m.counterErrorGlobal.count() : 0L;
        int totalTempsMig = (m.timerOkGlobal != null && m.timerOkGlobal.count() > 0) ?
                (int) m.timerOkGlobal.mean(TimeUnit.MILLISECONDS) : 0;
        Long peticionsOkUltimPeriode = m.timerOkLocal != null ? m.timerOkLocal.count() : 0L;
        Long peticionsErrorUltimPeriode = m.counterErrorLocal != null ? (long) m.counterErrorLocal.count() : 0L;
        int tempsMigUltimPeriode = (m.timerOkLocal != null && m.timerOkLocal.count() > 0) ?
                (int) m.timerOkLocal.mean(TimeUnit.MILLISECONDS) : 0;

		return IntegracioPeticions.builder()
			.totalOk(totalOk)
			.totalError(totalError)
			.totalTempsMig(totalTempsMig)
			.peticionsOkUltimPeriode(peticionsOkUltimPeriode)
			.peticionsErrorUltimPeriode(peticionsErrorUltimPeriode)
			.tempsMigUltimPeriode(tempsMigUltimPeriode)
			.build();
	}

	private String formatIntegracioCodi(IntegracioApp app) {
		return setFormatIntegracio(app.name());
	}

	private String setFormatIntegracio(String codiIntegracio) {
		return (codiIntegracio.length() > 16) ? codiIntegracio.substring(0, 16) : codiIntegracio;
	}

}