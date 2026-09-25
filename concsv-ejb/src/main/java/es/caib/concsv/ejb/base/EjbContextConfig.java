package es.caib.concsv.ejb.base;

import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * Creació del context Spring per a la capa dels EJBs.
 * <p>
 * En mode JBoss, els serveis del backoffice (Spring) viuen en aquest context i el WAR hi accedeix
 * a través dels EJBs que els envolten (veure {@link AbstractServiceEjb}). Els serveis CDI de
 * ConCSV no hi entren: no duen cap estereotip de Spring.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@EnableAutoConfiguration(excludeName = {
		"org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration",
		"org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration",
		"org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
		"org.springframework.boot.actuate.autoconfigure.metrics.jersey.JerseyServerMetricsAutoConfiguration"
})
@ComponentScan({
	BaseConfig.BASE_PACKAGE + ".logic",
	BaseConfig.BASE_PACKAGE + ".persist"
})
@PropertySource(ignoreResourceNotFound = true, value = {
		"classpath:application.properties",
		"file://${" + BaseConfig.APP_PROPERTIES + "}",
		"file://${" + BaseConfig.APP_SYSTEM_PROPERTIES + "}"})
public class EjbContextConfig {

	private static boolean initialized;
	private static ApplicationContext applicationContext;

	public static synchronized ApplicationContext getApplicationContext() {
		if (!initialized) {
			initialized = true;
			log.info("Starting EJB spring application...");
			applicationContext = new AnnotationConfigApplicationContext(EjbContextConfig.class);
			log.info("...EJB spring application started.");
		}
		return applicationContext;
	}

	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasename("classpath:" + BaseConfig.APP_NAME + "-service-messages");
		messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}

}
