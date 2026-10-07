package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.model.ResourceReference;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Grup de propietats configurables. Els grups formen un arbre d'un sol nivell (arrel i fills).
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = ConfigGroupResource.Fields.description,
		quickFilterFields = { ConfigGroupResource.Fields.key, ConfigGroupResource.Fields.description },
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
				roles = { BaseConfig.ROLE_SUPER },
				grantedPermissions = { PermissionEnum.READ }
		)
)
public class ConfigGroupResource extends BaseResource<String> {

	private String key;
	private String description;
	private int position;
	private ResourceReference<ConfigGroupResource, String> parent;

	@Override
	public String getId() {
		return key;
	}

}
