/**
 * 
 */
package es.caib.concsv.front.config;

import es.caib.concsv.service.facade.EstadisticaServiceInterface;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;

import es.caib.concsv.service.facade.HashServiceInterface;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuració d'accés als EJBs.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Configuration
@ConditionalOnWarDeployment
public class EjbClientConfig {

	static final String EJB_JNDI_PREFIX = "java:app/" + BaseConfig.APP_NAME + "-ejb/";
	static final String EJB_JNDI_SUFFIX = "";
	static final String EJB_CLASS_DELETE_SUFFIX = "Interface";

	@Bean
	public LocalStatelessSessionProxyFactoryBean hashService() {
		return getLocalEjbFactoyBean(HashServiceInterface.class);
	}

    @Bean
    public LocalStatelessSessionProxyFactoryBean estadisticaService() {
        return getLocalEjbFactoyBean(EstadisticaServiceInterface.class);
    }

	private LocalStatelessSessionProxyFactoryBean getLocalEjbFactoyBean(Class<?> serviceClass) {
		String jndiName = jndiServiceName(serviceClass, false);
		log.info("Creating EJB proxy for " + serviceClass.getSimpleName() + " with JNDI name " + jndiName);
		LocalStatelessSessionProxyFactoryBean factoryBean = new LocalStatelessSessionProxyFactoryBean();
		factoryBean.setBusinessInterface(serviceClass);
		factoryBean.setExpectedType(serviceClass);
		factoryBean.setJndiName(jndiName);
		return factoryBean;
	}

	private String jndiServiceName(Class<?> serviceClass, boolean addServiceClassName) {
		String serviceClassName = serviceClass.getSimpleName();
		if (!EJB_CLASS_DELETE_SUFFIX.isBlank()) {
			int suffixIndex = serviceClassName.indexOf(EJB_CLASS_DELETE_SUFFIX);
			if (suffixIndex != -1) {
				serviceClassName = serviceClassName.substring(0, suffixIndex);
			}
		}
		return EJB_JNDI_PREFIX + serviceClassName + EJB_JNDI_SUFFIX + (addServiceClassName ? "!" + serviceClass.getName() : "");
	}

}
