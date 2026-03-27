/**
 * 
 */
package es.caib.concsv.front.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Configuració de les propietats de l'aplicació a partir de les propietats de
 * sistema (System.getProperty).
 * 
 * @author Límit Tecnologies
 */
@Configuration
@ConditionalOnWarDeployment
@PropertySource(ignoreResourceNotFound = true, value = {
	"file://${" + BaseConfig.APP_PROPERTIES + "}",
	"file://${" + BaseConfig.APP_SYSTEM_PROPERTIES + "}"})
public class SystemPropertiesConfig {

}
