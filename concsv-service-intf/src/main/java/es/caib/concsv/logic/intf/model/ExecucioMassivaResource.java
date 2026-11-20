package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceArtifact;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.model.ResourceArtifactType;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Execució massiva: una acció que es fa en segon pla sobre molts elements (ara, la consulta de les
 * dades dels documents exclosos). Equivalent a {@code ExecucioMassivaResource} de RIPEA.
 * <p>
 * És un recurs de només lectura, només visible pel superusuari: les execucions les crea l'acció
 * massiva que les origina i les escriu el procés en segon pla; des d'aquí només es poden veure i
 * cancel·lar.
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = ExecucioMassivaResource.Fields.tipus,
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
				roles = { BaseConfig.ROLE_SUPER },
				grantedPermissions = { PermissionEnum.READ }
		),
		artifacts = {
				// Cancel·lació d'una execució pendent o en curs. Com a recurs de només lectura, l'acció
				// necessita el seu propi permís.
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = ExecucioMassivaResource.ACTION_CANCELAR_CODE,
						requiresId = true,
						accessConstraints = @ResourceAccessConstraint(
								type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
								roles = { BaseConfig.ROLE_SUPER },
								grantedPermissions = { PermissionEnum.READ }
						))
		}
)
public class ExecucioMassivaResource extends BaseResource<Long> {

	public static final String ACTION_CANCELAR_CODE = "CANCELAR";

	/** Resultat de {@link #ACTION_CANCELAR_CODE} quan s'ha cancel·lat a l'instant (cap node no l'havia agafada). */
	public static final String RESULTAT_CANCELADA = "CANCELADA";
	/** Resultat de {@link #ACTION_CANCELAR_CODE} quan s'ha demanat la cancel·lació d'una execució en curs. */
	public static final String RESULTAT_EN_CURS = "EN_CURS";

	private ExecucioMassivaTipusEnum tipus;
	private LocalDateTime dataInici;
	private LocalDateTime dataFi;
	/** Si s'ha d'avisar per correu l'usuari que l'ha creada quan acabi. */
	private Boolean enviarCorreu;
	private ExecucioMassivaEstatEnum estat;
	private LocalDateTime cancelacioData;
	private String cancelacioUsuari;
	/** Codi (identificador) de l'usuari que ha creat l'execució: és el que es desa a la BD. */
	private String createdBy;
	/** L'usuari que l'ha creada com es mostra: "Nom (codi)". Es calcula a partir del perfil, no es persisteix. */
	@Transient
	private String createdByFullName;
	private LocalDateTime createdDate;

	// Recomptes dels elements: es calculen, no es persisteixen.

	/** Nombre total d'elements. */
	@Transient
	private int total;
	@Transient
	private int finalitzats;
	@Transient
	private int errors;
	@Transient
	private int pendents;
	@Transient
	private int cancelats;
	/** Si es pot cancel·lar ara: ni ha acabat ni s'ha demanat ja la cancel·lació, i el tipus ho permet. */
	@Transient
	private boolean cancelable;

	/**
	 * Resultat de {@link #ACTION_CANCELAR_CODE}. Va dins un objecte perquè la resposta de l'acció es llegeix
	 * com a JSON, i un text sol no ho és.
	 */
	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ResultatCancelacio implements Serializable {
		private static final long serialVersionUID = 1L;
		/** {@link #RESULTAT_CANCELADA} o {@link #RESULTAT_EN_CURS}. */
		private String resultat;
	}

}
