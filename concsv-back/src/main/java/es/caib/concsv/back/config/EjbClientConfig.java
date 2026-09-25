package es.caib.concsv.back.config;

import es.caib.concsv.logic.intf.base.service.PermissionEvaluatorService;
import es.caib.concsv.logic.intf.base.service.ResourceApiService;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.resourceservice.AvisResourceService;
import es.caib.concsv.logic.intf.resourceservice.EntitatResourceService;
import es.caib.concsv.logic.intf.resourceservice.UsuariResourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWarDeployment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ejb.access.LocalStatelessSessionProxyFactoryBean;

/**
 * Configuració d'accés als serveis de Spring mitjançant EJBs (només en mode JBoss).
 * <p>
 * En mode Spring Boot els serveis són beans del mateix context i aquests proxies no es creen
 * ({@link ConditionalOnWarDeployment}). El nom JNDI és el nom simple de la interfície amb el
 * sufix "Ejb" (veure els EJBs de concsv-ejb). Els noms dels mètodes {@code permissionEvaluatorService}
 * i {@code resourceApiService} són els que esperen les classes base (p. ex. el
 * {@code @DependsOn} de {@code MethodSecurityConfig}).
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Configuration
public class EjbClientConfig {

	static final String EJB_JNDI_PREFIX = "java:app/" + BaseConfig.APP_NAME + "-ejb/";
	static final String EJB_JNDI_SUFFIX = "Ejb";

	@Bean
	@ConditionalOnWarDeployment
	public LocalStatelessSessionProxyFactoryBean permissionEvaluatorService() {
		return getLocalEjbFactoyBean(PermissionEvaluatorService.class);
	}

	@Bean
	@ConditionalOnWarDeployment
	public LocalStatelessSessionProxyFactoryBean resourceApiService() {
		return getLocalEjbFactoyBean(ResourceApiService.class);
	}

	@Bean
	@ConditionalOnWarDeployment
	public LocalStatelessSessionProxyFactoryBean entitatResourceService() {
		return getLocalEjbFactoyBean(EntitatResourceService.class);
	}

	@Bean
	@ConditionalOnWarDeployment
	public LocalStatelessSessionProxyFactoryBean usuariResourceService() {
		return getLocalEjbFactoyBean(UsuariResourceService.class);
	}

	@Bean
	@ConditionalOnWarDeployment
	public LocalStatelessSessionProxyFactoryBean avisResourceService() {
		return getLocalEjbFactoyBean(AvisResourceService.class);
	}

	private LocalStatelessSessionProxyFactoryBean getLocalEjbFactoyBean(Class<?> serviceClass) {
		String jndiName = EJB_JNDI_PREFIX + serviceClass.getSimpleName() + EJB_JNDI_SUFFIX;
		log.info("Creating EJB proxy for " + serviceClass.getSimpleName() + " with JNDI name " + jndiName);
		LocalStatelessSessionProxyFactoryBean factoryBean = new LocalStatelessSessionProxyFactoryBean();
		factoryBean.setBusinessInterface(serviceClass);
		factoryBean.setExpectedType(serviceClass);
		factoryBean.setJndiName(jndiName);
		return factoryBean;
	}

}
