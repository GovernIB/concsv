package es.caib.concsv.ejb.base;

import es.caib.concsv.logic.intf.base.model.Resource;
import es.caib.concsv.logic.intf.base.permission.ResourcePermissions;
import es.caib.concsv.logic.intf.base.service.ResourceApiService;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.List;

/**
 * EJB que exposa al WAR del backoffice (mode JBoss) el registre de recursos de l'API REST.
 * <p>
 * {@code @PermitAll} perquè els controladors hi registren els seus recursos en arrencar el WAR,
 * quan encara no hi ha cap usuari autenticat.
 *
 * @author Límit Tecnologies
 */
@Local(ResourceApiService.class)
@Stateless
@PermitAll
public class ResourceApiServiceEjb extends AbstractServiceEjb<ResourceApiService> implements ResourceApiService {

	private ResourceApiService delegateService;

	@Override
	protected void setDelegateService(ResourceApiService delegateService) {
		this.delegateService = delegateService;
	}

	@Override
	public void resourceRegister(Class<? extends Resource<?>> resourceClass) {
		delegateService.resourceRegister(resourceClass);
	}

	@Override
	public List<Class<? extends Resource<?>>> resourceFindAllowed() {
		return delegateService.resourceFindAllowed();
	}

	@Override
	public ResourcePermissions permissionsCurrentUser(Class<?> resourceClass, Serializable resourceId) {
		return delegateService.permissionsCurrentUser(resourceClass, resourceId);
	}

}
