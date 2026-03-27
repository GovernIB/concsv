package es.caib.concsv.ejb.helpers;

import es.caib.comanda.model.v1.salut.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@ApplicationScoped
public class SubsistemesHelper {

	// Llindars d'avís en percentatge (0-100)
	private static final int DOWN_PCT = 100;     // 100% errors
	private static final int ERROR_GT_PCT = 30;  // >30% errors
	private static final int DEGRADED_GT_PCT = 10; // >10% errors
	private static final int UP_LT_PCT = 5;      // <5% errors

	@Inject @Setter private MeterRegistry registry; // Global
	private MeterRegistry localRegistry; // Local

	private final Map<SubsistemesEnum, Metrics> metrics = new EnumMap<>(SubsistemesEnum.class);

	private static class Metrics {
		Timer timerOkGlobal;
		Counter counterErrorGlobal;
		Timer timerOkLocal;
		Counter counterErrorLocal;
		EstatSalutEnum darrerEstat = EstatSalutEnum.UNKNOWN;
	}

	@PostConstruct
	public void init() {
		if (registry == null) {
			log.warn("MeterRegistry no inicialitzat. No es registraran mètriques globals fins que s'estableixi el registry.");
			return;
		}
		initializeMetrics();
	}

	private void initializeMetrics() {
		// Reset local registry
		if (localRegistry != null) {
			try {
				localRegistry.close();
			} catch (Exception ignore) {
			}
		}
		localRegistry = new SimpleMeterRegistry();

		for (SubsistemesEnum s : SubsistemesEnum.values()) {
			Metrics m = metrics.computeIfAbsent(s, k -> new Metrics());

			// Globals timers/counters
			if (registry != null && m.timerOkGlobal == null) {
				m.timerOkGlobal = Timer.builder("subsistema." + s.name().toLowerCase())
					.tags("result", "success")
					.publishPercentiles(0.5, 0.75, 0.95, 0.99)
					.publishPercentileHistogram()
					.register(registry);
				m.counterErrorGlobal = Counter.builder("subsistema." + s.name().toLowerCase() + ".errors")
					.register(registry);
			}

			// Locals timers/counters
			m.timerOkLocal = Timer.builder("subsistema." + s.name().toLowerCase() + ".local")
				.tags("result", "success")
				.publishPercentiles(0.5, 0.75, 0.95, 0.99)
				.publishPercentileHistogram()
				.register(localRegistry);
			m.counterErrorLocal = Counter.builder("subsistema." + s.name().toLowerCase() + ".local.errors")
				.register(localRegistry);
		}
	}

	private void resetLocalTimers() {
		initializeMetrics();
	}

	@Getter
	public enum SubsistemesEnum {
		EXC("Documents exclosos", false),
		CHE("CheckHash", false),
		MET("Obtenir metadades", false),
		ORI("Obtenir document original", true),
		IMP("Obtenir document imprimible", true),
		ENI("Obtenir ENI document", false),
		;

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
		Metrics m = metrics.computeIfAbsent(subsistema, k -> new Metrics());
		if (m.timerOkGlobal != null) {
			m.timerOkGlobal.record(duracio, TimeUnit.MILLISECONDS);
		}
		if (m.timerOkLocal != null) {
			m.timerOkLocal.record(duracio, TimeUnit.MILLISECONDS);
		}
	}

	/** Registra una operació errònia (global + local) **/
	public void addErrorOperation(SubsistemesEnum subsistema) {
		Metrics m = metrics.computeIfAbsent(subsistema, k -> new Metrics());
		if (m.counterErrorGlobal != null) {
			m.counterErrorGlobal.increment();
		}
		if (m.counterErrorLocal != null) {
			m.counterErrorLocal.increment();
		}
	}

	/** Retorna informació de salut bàsica dels subsistemes **/
	public SubsistemesInfo getSubsistemesInfo() {
		final List<SubsistemaSalut> subsistemasSalut = getSubsistemesSalut();
		final EstatSalutEnum estatGlobal = calculateGlobalHealth(subsistemasSalut);
		return SubsistemesInfo.builder()
			.subsistemesSalut(subsistemasSalut)
			.estatGlobal(estatGlobal)
			.build();
	}

	private List<SubsistemaSalut> getSubsistemesSalut() {
		List<SubsistemaSalut> subsistemasSalut = new ArrayList<>();

		for (SubsistemesEnum s : SubsistemesEnum.values()) {
			Metrics m = metrics.computeIfAbsent(s, k -> new Metrics());

            final int latencia = (m.timerOkLocal != null && m.timerOkLocal.count() > 0) ?
                    (int) m.timerOkLocal.mean(TimeUnit.MILLISECONDS) : 0;
            final Long totalOk = m.timerOkGlobal != null ? m.timerOkGlobal.count() : 0L;
            final Long totalError = m.counterErrorGlobal != null ? (long) m.counterErrorGlobal.count() : 0L;
            final int totalTempsMig = (m.timerOkGlobal != null && m.timerOkGlobal.count() > 0) ?
                    (int) m.timerOkGlobal.mean(TimeUnit.MILLISECONDS) : 0;
            Long peticionsOkUltimPeriode = m.timerOkLocal != null ? m.timerOkLocal.count() : 0L;
            Long peticionsErrorUltimPeriode = m.counterErrorLocal != null ? (long) m.counterErrorLocal.count() : 0L;

			final EstatSalutEnum estat = calculaEstat(totalOk, totalError, s);

			subsistemasSalut.add(SubsistemaSalut.builder()
				.codi(s.name())
				.latencia(latencia)
				.estat(estat)
				.totalOk(totalOk)
				.totalError(totalError)
                .totalTempsMig(totalTempsMig)
                .peticionsOkUltimPeriode(peticionsOkUltimPeriode)
                .peticionsErrorUltimPeriode(peticionsErrorUltimPeriode)
                .tempsMigUltimPeriode(latencia)
				.build());
		}

		resetLocalTimers();
		return subsistemasSalut;
	}

	private EstatSalutEnum calculaEstat(Long totalPeticionsOk, Long totalPeticionsError, SubsistemesEnum subsistema) {
		final long ok = (totalPeticionsOk != null) ? totalPeticionsOk : 0L;
		final long ko = (totalPeticionsError != null) ? totalPeticionsError : 0L;
		final long total = ok + ko;

		if (total == 0L) {
			return getDarrerEstat(subsistema);
		}

		final int errorRatePct = (int) Math.round((ko * 100.0) / Math.max(1L, total));

		EstatSalutEnum estat;
		if (errorRatePct >= DOWN_PCT) {
			estat = EstatSalutEnum.DOWN;
		} else if (errorRatePct > ERROR_GT_PCT) {
			estat = EstatSalutEnum.ERROR;
		} else if (errorRatePct > DEGRADED_GT_PCT) {
			estat = EstatSalutEnum.DEGRADED;
		} else if (errorRatePct < UP_LT_PCT) {
			estat = EstatSalutEnum.UP;
		} else {
			estat = EstatSalutEnum.WARN;
		}

		setDarrerEstat(subsistema, estat);
		return estat;
	}

	private EstatSalutEnum getDarrerEstat(SubsistemesEnum subsistema) {
		Metrics m = metrics.get(subsistema);
		return m != null && m.darrerEstat != null ? m.darrerEstat : EstatSalutEnum.UNKNOWN;
	}

	private void setDarrerEstat(SubsistemesEnum subsistema, EstatSalutEnum estat) {
		Metrics m = metrics.computeIfAbsent(subsistema, k -> new Metrics());
		m.darrerEstat = estat;
	}

	private EstatSalutEnum calculateGlobalHealth(List<SubsistemaSalut> subsistemes) {
		boolean anyDown = false, anyError = false, anyDegraded = false, anyWarn = false, anyUp = false;
		for (SubsistemaSalut s : subsistemes) {
			switch (s.getEstat()) {
				case UP: anyUp = true; break;
				case WARN: anyWarn = true; break;
				case DEGRADED: anyDegraded = true; break;
				case ERROR: anyError = true; break;
				case DOWN:
					SubsistemesEnum subsistemesEnum = SubsistemesEnum.valueOf(s.getCodi());
					if (subsistemesEnum.isSistemaCritic()) anyDown = true;
					else anyError = true;
					break;
				default: // UNKNOWN
			}
		}
		if (anyDown) return EstatSalutEnum.DOWN;
		if (anyError) return EstatSalutEnum.ERROR;
		if (anyDegraded) return EstatSalutEnum.DEGRADED;
		if (anyWarn) return EstatSalutEnum.WARN;
		if (anyUp) return EstatSalutEnum.UP;
		return EstatSalutEnum.UNKNOWN;
	}

	@Getter
	@Builder
	public static class SubsistemesInfo {
		private final List<SubsistemaSalut> subsistemesSalut;
		private final EstatSalutEnum estatGlobal;
	}
}
