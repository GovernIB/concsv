/**
 * 
 */
package es.caib.concsv.front.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.hateoas.mediatype.hal.forms.HalFormsConfiguration;

/**
 * Configuració de Spring HATEOAS.
 * 
 * @author Límit Tecnologies
 */
@Configuration
@EnableHypermediaSupport(type = { HypermediaType.HAL, HypermediaType.HAL_FORMS })
public class HateoasConfig {

	@Bean
	HalFormsConfiguration halFormsConfiguration() {
		HalFormsConfiguration configuration = new HalFormsConfiguration();
		return configuration;
	}

}
