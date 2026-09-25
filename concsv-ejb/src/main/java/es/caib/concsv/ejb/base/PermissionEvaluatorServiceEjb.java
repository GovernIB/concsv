package es.caib.concsv.ejb.base;

import es.caib.concsv.logic.intf.base.service.PermissionEvaluatorService;
import org.springframework.security.core.Authentication;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import java.io.Serializable;

/**
 * EJB que exposa al WAR del backoffice (mode JBoss) l'avaluador de permisos de la seguretat de
 * mètodes (veure {@code MethodSecurityConfig}). L'autenticació arriba com a paràmetre, per això
 * l'EJB no restringeix l'accés.
 *
 * @author Límit Tecnologies
 */
@Local(PermissionEvaluatorService.class)
@Stateless
@PermitAll
public class PermissionEvaluatorServiceEjb extends AbstractServiceEjb<PermissionEvaluatorService> implements PermissionEvaluatorService {

	private PermissionEvaluatorService delegateService;

	@Override
	protected void setDelegateService(PermissionEvaluatorService delegateService) {
		this.delegateService = delegateService;
	}

	@Override
	public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
		return delegateService.hasPermission(authentication, targetDomainObject, permission);
	}

	@Override
	public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
		return delegateService.hasPermission(authentication, targetId, targetType, permission);
	}

}
