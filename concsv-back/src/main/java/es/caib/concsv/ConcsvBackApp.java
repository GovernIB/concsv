package es.caib.concsv;

import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Classe principal del backoffice de ConCSV desplegat com a WAR dins l'EAR (JBoss).
 * <p>
 * Només escaneja la capa web: els serveis (capes logic i persist) viuen al context Spring dels
 * EJBs i s'hi accedeix a través dels proxies d'{@code EjbClientConfig}. Per a executar-ho tot en
 * un sol procés s'ha d'usar {@link ConcsvBackBootApp}.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@ConditionalOnWarDeployment
@SpringBootApplication(
		exclude = {
				DataSourceAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				JpaRepositoriesAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class,
				TransactionAutoConfiguration.class,
				LiquibaseAutoConfiguration.class,
				FreeMarkerAutoConfiguration.class,
				WebSocketServletAutoConfiguration.class
		},
		excludeName = {
				"org.springframework.boot.actuate.autoconfigure.metrics.jersey.JerseyServerMetricsAutoConfiguration"
		})
@ComponentScan(BaseConfig.BASE_PACKAGE + ".back")
@PropertySource(
		ignoreResourceNotFound = true,
		value = { "classpath:application.properties" })
public class ConcsvBackApp extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(ConcsvBackApp.class, args);
	}

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		try (InputStream manifestStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF")) {
			if (manifestStream == null) {
				log.warn("No s'ha trobat l'arxiu MANIFEST.MF");
			} else {
				Attributes attributes = new Manifest(manifestStream).getMainAttributes();
				log.info("Carregant el backoffice de ConCSV versió " + attributes.getValue("Implementation-Version") +
						" generada en data " + attributes.getValue("Build-Timestamp"));
			}
		} catch (IOException ex) {
			throw new ServletException("No s'ha pogut llegir l'arxiu MANIFEST.MF", ex);
		}
		super.onStartup(servletContext);
	}

}
