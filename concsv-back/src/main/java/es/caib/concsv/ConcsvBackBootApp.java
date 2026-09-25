package es.caib.concsv;

import es.caib.concsv.logic.intf.config.BaseConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

/**
 * Classe principal del backoffice de ConCSV per a executar amb Spring Boot, sense JBoss: la capa
 * web i els serveis (capes logic i persist) viuen al mateix context. Requereix el perfil Maven
 * {@code boot} (o {@code eclipse}), que afegeix concsv-service al classpath.
 * <p>
 * L'escaneig es limita als paquets de Spring: {@code es.caib.concsv} inclou també classes CDI
 * (p. ex. {@code commons.utils.Version}, anotada amb {@code @Named}) que Spring també
 * consideraria components.
 *
 * @author Límit Tecnologies
 */
@SpringBootApplication(excludeName = {
		"org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration",
		"org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration"
})
@ComponentScan({
		BaseConfig.BASE_PACKAGE + ".back",
		BaseConfig.BASE_PACKAGE + ".logic",
		BaseConfig.BASE_PACKAGE + ".persist"
})
@PropertySource(
		ignoreResourceNotFound = true,
		value = { "classpath:application.properties" })
public class ConcsvBackBootApp {

	public static void main(String[] args) {
		SpringApplication.run(ConcsvBackBootApp.class, args);
	}

}
