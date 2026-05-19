package es.caib.concsv.logic.producer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

@ApplicationScoped
public class MetricsProducer {

	@Produces
	@Singleton
	public MeterRegistry produceMeterRegistry() {
		return new SimpleMeterRegistry();
	}

}
