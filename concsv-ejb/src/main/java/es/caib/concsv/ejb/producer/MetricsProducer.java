package es.caib.concsv.ejb.producer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

public class MetricsProducer {

	@Produces
	@Singleton
	public MeterRegistry produceMeterRegistry() {
		return new SimpleMeterRegistry();
	}
}