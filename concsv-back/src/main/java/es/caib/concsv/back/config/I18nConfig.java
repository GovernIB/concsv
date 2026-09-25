/**
 * 
 */
package es.caib.concsv.back.config;

import java.util.Locale;

import org.springframework.context.annotation.Configuration;

import es.caib.concsv.back.base.config.BaseMessageSourceConfig;
import es.caib.concsv.logic.intf.config.BaseConfig;

/**
 * Configuració multiidioma de l'aplicació.
 *
 * @author Límit Tecnologies
 */
@Configuration
public class I18nConfig extends BaseMessageSourceConfig {

	/**
	 * Bundle de missatges de l'aplicació ({@code concsv-service-messages}, al mòdul
	 * concsv-service-intf). És el mateix que fa servir el context Spring dels EJBs
	 * ({@code EjbContextConfig.messageSource()}), així que les dues modalitats d'execució (Spring
	 * Boot i JBoss) veuen sempre els mateixos textos. A concsv-back només hi queden els
	 * {@code _prompt} del motor genèric de recursos ({@code concsv-back-rest-messages}).
	 */
	@Override
	protected String[] getBasenames() {
		return new String[] {
				"concsv-service-messages"
		};
	}

	@Override
	protected Locale getDefaultLocale() {
		return Locale.forLanguageTag(BaseConfig.DEFAULT_LOCALE);
	}

}
