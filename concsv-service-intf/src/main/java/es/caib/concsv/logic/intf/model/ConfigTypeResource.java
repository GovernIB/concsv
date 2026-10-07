package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

/**
 * Tipus de dada d'una propietat configurable (BOOL, TEXT, INT, CREDENTIALS...). El camp
 * {@code value} conté, quan el tipus és una enumeració, la llista de valors vàlids separats per
 * coma; el frontal el rep com a descripció de la referència.
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = ConfigTypeResource.Fields.value,
		quickFilterFields = { ConfigTypeResource.Fields.code, ConfigTypeResource.Fields.value },
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
				roles = { BaseConfig.ROLE_SUPER },
				grantedPermissions = { PermissionEnum.READ }
		)
)
public class ConfigTypeResource extends BaseResource<String> {

	private String code;
	private String value;

	@Override
	public String getId() {
		return code;
	}

}
