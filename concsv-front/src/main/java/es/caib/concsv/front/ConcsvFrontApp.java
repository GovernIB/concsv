package es.caib.concsv.front;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.filter.ForwardedHeaderFilter;

import lombok.extern.slf4j.Slf4j;

/**
 * Aplicació de consulta de documents amb codi CSV (concsv).
 * 
 * @author Limit Tecnologies <limit@limit.es>
 */
@Slf4j
@EnableScheduling
@ComponentScan({ "es.caib.concsv.front" })
@SpringBootApplication(exclude = {
		RepositoryRestMvcAutoConfiguration.class,
		DataSourceAutoConfiguration.class, 
		DataSourceTransactionManagerAutoConfiguration.class,
		JpaRepositoriesAutoConfiguration.class,
		HibernateJpaAutoConfiguration.class,
		TransactionAutoConfiguration.class,
		LiquibaseAutoConfiguration.class,
		FreeMarkerAutoConfiguration.class,
		WebSocketServletAutoConfiguration.class
})
public class ConcsvFrontApp extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(
				ConcsvFrontApp.class,
				args);
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		try {
			Manifest manifest = new Manifest(servletContext.getResourceAsStream("/META-INF/MANIFEST.MF"));
			Attributes attributes = manifest.getMainAttributes();
			String version = attributes.getValue("Implementation-Version");
			String buildTimestamp = attributes.getValue("Build-Timestamp");
			log.info("Carregant l'aplicació ConCSV versió " + version + " generada en data " + buildTimestamp);
		} catch (IOException ex) {
			throw new ServletException("Couldn't read MANIFEST.MF", ex);
		}
		super.onStartup(servletContext);
	}

	// Filtre per a processar les capçaleres HTTP X-Forwarded-* i permetre que Spring
	// Data REST generi correctament els links dels recursos quan l'aplicació està
	// desplegada darrera un proxy HTTP.
	@Bean
	public Filter forwardedHeaderFilter() {
		return new ForwardedHeaderFilter();
	}

}
