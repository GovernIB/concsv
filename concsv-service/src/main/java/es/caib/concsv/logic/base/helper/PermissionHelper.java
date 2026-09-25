package es.caib.concsv.logic.base.helper;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.model.ResourceArtifactType;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Mètodes per a la comprovació de permisos.
 * <p>
 * ConCSV no té de moment cap restricció d'accés de tipus CUSTOM: tots els recursos es protegeixen
 * per rol (veure els {@code @ResourceAccessConstraint} de cada classe *Resource).
 *
 * @author Límit Tecnologies
 */
@Component
public class PermissionHelper extends BasePermissionHelper {

	@Override
	protected boolean checkCustomResourceAccessConstraint(
			Authentication auth,
			Serializable resourceId,
			Class<?> resourceClass,
			ResourceAccessConstraint resourceAccessConstraint,
			BasePermission[] permissions) {
		return false;
	}

	@Override
	protected boolean checkCustomResourceArtifactAccessConstraint(
			Authentication auth,
			Class<?> resourceClass,
			ResourceArtifactType type,
			String code,
			ResourceAccessConstraint resourceAccessConstraint) {
		return false;
	}

}
