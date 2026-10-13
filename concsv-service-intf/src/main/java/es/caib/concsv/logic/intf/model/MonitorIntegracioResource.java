package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceArtifact;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.model.ResourceArtifactType;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Petició a una integració externa (Arxiu, Custòdia Antiga, validació de firmes o conversió de
 * documents) enregistrada pel
 * monitor d'integracions. És un recurs de només lectura, només visible pel superusuari: els
 * registres els escriu l'aplicació, mai el backoffice.
 * <p>
 * Mateixos camps que {@code IntegracioResource} de RIPEA, sense l'entitat (el camí públic de
 * consulta és anònim): {@code entitatCodi} només es manté per paritat i normalment és buit.
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = MonitorIntegracioResource.Fields.descripcio,
		quickFilterFields = { MonitorIntegracioResource.Fields.descripcio },
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
				roles = { BaseConfig.ROLE_SUPER },
				grantedPermissions = { PermissionEnum.READ }
		),
		artifacts = {
				@ResourceArtifact(
						type = ResourceArtifactType.FILTER,
						code = MonitorIntegracioResource.FILTER_CODE,
						formClass = MonitorIntegracioResource.FormFilter.class)
		}
)
public class MonitorIntegracioResource extends BaseResource<Long> {

	public static final String FILTER_CODE = "FILTER";

	private Date data;
	private IntegracioCodiEnum codi;
	private String descripcio;
	private String endpoint;
	/** Paràmetres de la petició (p. ex. hash o uuid), desats com a JSON a la base de dades. */
	private Map<String, String> parametres;
	private IntegracioAccioTipusEnum tipus;
	/** Temps de resposta en mil·lisegons. */
	private Long tempsResposta;
	private IntegracioAccioEstatEnum estat;
	private String entitatCodi;
	private String errorDescripcio;
	private String excepcioMessage;
	private String excepcioStacktrace;

	/**
	 * Formulari del filtre del llistat. El camp {@code codi} no hi és: la pantalla el filtra amb
	 * les pestanyes de cada integració.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@FieldNameConstants
	public static class FormFilter implements Serializable {

		private static final long serialVersionUID = 1L;

		private IntegracioAccioEstatEnum estat;
		private IntegracioAccioTipusEnum tipus;
		private String descripcio;
		private Date dataInici;
		private Date dataFi;

	}

}
