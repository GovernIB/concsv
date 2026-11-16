package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceArtifact;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.model.ResourceArtifactType;
import es.caib.concsv.logic.intf.base.model.ResourceReference;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Propietat configurable de l'aplicació.
 * <p>
 * Les files que tenen {@code jbossProperty} es llegeixen del fitxer de propietats del servidor
 * (el valor que es mostra és el valor efectiu) i només es poden consultar. La resta es poden
 * modificar: l'aplicació usa el valor de la base de dades i, si és nul, el del fitxer del servidor.
 * <p>
 * Les files no es creen ni s'esborren des del backoffice: les defineix l'script SQL de la versió.
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = ConfigResource.Fields.description,
		// El valor no hi és: el filtre del servidor permetria endevinar el contingut d'una credencial.
		quickFilterFields = { ConfigResource.Fields.key, ConfigResource.Fields.description },
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
				roles = { BaseConfig.ROLE_SUPER },
				grantedPermissions = { PermissionEnum.READ, PermissionEnum.WRITE }
		),
		artifacts = {
				// Copia a la base de dades el valor dels fitxers del servidor. Sense formClass ni id:
				// s'executa sobre tot el conjunt de propietats. Sense accessConstraints pròpies
				// requereix el permís WRITE sobre el recurs, és a dir CSV_SUPER.
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = ConfigResource.ACTION_SYNC_JBOSS_CODE,
						requiresId = false)
		}
)
public class ConfigResource extends BaseResource<String> {

	public static final String ACTION_SYNC_JBOSS_CODE = "SYNC_JBOSS";

	@NotNull
	private String key;
	@Size(max = 2048)
	private String value;
	@Size(max = 2048)
	private String description;
	private boolean jbossProperty;
	@NotNull
	private ResourceReference<ConfigGroupResource, String> group;
	@NotNull
	private ResourceReference<ConfigTypeResource, String> type;
	private int position;
	/**
	 * Només es fixa en llegir el recurs (veure el servei): el valor del fitxer del servidor a les
	 * propietats editables que no tenen valor a la base de dades. No es persisteix.
	 */
	private String alternativeValue;
	private String lastModifiedBy;
	private LocalDateTime lastModifiedDate;

	@Override
	public String getId() {
		return key;
	}

}
