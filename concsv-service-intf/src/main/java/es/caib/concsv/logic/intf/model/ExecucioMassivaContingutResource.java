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

import java.time.LocalDateTime;

/**
 * Element d'una {@link ExecucioMassivaResource}: el document o altre objecte sobre el qual s'ha de fer
 * l'acció, amb el seu estat i, si ha fallat, el motiu. Equivalent a {@code ExecucioMassivaContingutResource}
 * de RIPEA. Recurs de només lectura.
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = ExecucioMassivaContingutResource.Fields.elementNom,
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
				roles = { BaseConfig.ROLE_SUPER },
				grantedPermissions = { PermissionEnum.READ }
		)
)
public class ExecucioMassivaContingutResource extends BaseResource<Long> {

	private ResourceReference<ExecucioMassivaResource, Long> execucioMassiva;
	private LocalDateTime dataInici;
	private LocalDateTime dataFi;
	private ExecucioMassivaContingutEstatEnum estat;
	/** Motiu de l'error, si l'element ha acabat en {@code ERROR}. */
	private String error;
	/** Ordre de tractament dins l'execució. */
	private int ordre;
	private ElementTipusEnum elementTipus;
	private Long elementId;
	/** Nom de l'element quan es va crear l'execució (per als documents exclosos, el seu UUID o CSV). */
	private String elementNom;

}
