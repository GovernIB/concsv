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

import java.io.Serializable;
import java.util.Date;
import java.util.List;
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
						formClass = MonitorIntegracioResource.FormFilter.class),
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = MonitorIntegracioResource.ACTION_PIPELLES_CODE,
						requiresId = false,
						accessConstraints = @ResourceAccessConstraint(
								type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
								roles = { BaseConfig.ROLE_SUPER },
								grantedPermissions = { PermissionEnum.READ }
						)),
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = MonitorIntegracioResource.ACTION_INTEGRACIONS_DIAGNOSTIC_CODE,
						requiresId = false,
						accessConstraints = @ResourceAccessConstraint(
								type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
								roles = { BaseConfig.ROLE_SUPER },
								grantedPermissions = { PermissionEnum.READ }
						)),
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = MonitorIntegracioResource.ACTION_DIAGNOSTIC_CODE,
						formClass = MonitorIntegracioResource.DiagnosticForm.class,
						requiresId = false,
						accessConstraints = @ResourceAccessConstraint(
								type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
								roles = { BaseConfig.ROLE_SUPER },
								grantedPermissions = { PermissionEnum.READ }
						))
		}
)
public class MonitorIntegracioResource extends BaseResource<Long> {

	public static final String FILTER_CODE = "FILTER";
	public static final String ACTION_INTEGRACIONS_DIAGNOSTIC_CODE = "INTEGRACIONS_DIAGNOSTIC";
	public static final String ACTION_DIAGNOSTIC_CODE = "DIAGNOSTIC";
	public static final String ACTION_PIPELLES_CODE = "PIPELLES";

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

	/**
	 * Resposta de l'acció {@link #ACTION_INTEGRACIONS_DIAGNOSTIC_CODE}: els codis de les
	 * integracions que es poden diagnosticar.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class IntegracionsDiagnosticResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<String> codis;

	}

	/**
	 * Paràmetres de l'acció {@link #ACTION_DIAGNOSTIC_CODE}: el codi de la integració a provar,
	 * un dels retornats per {@link #ACTION_INTEGRACIONS_DIAGNOSTIC_CODE}.
	 */
	@Getter
	@Setter
	public static class DiagnosticForm implements Serializable {

		private static final long serialVersionUID = 1L;

		private String codiIntegracio;

	}

	/**
	 * Resposta de l'acció {@link #ACTION_DIAGNOSTIC_CODE}. Una prova que falla no és un error de
	 * l'acció: es retorna amb {@code correcte = false} i el motiu a {@code errMsg}.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DiagnosticResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private boolean correcte;
		private String prova;
		private String errMsg;
		/** Traça de l'excepció si la prova ha fallat amb una. */
		private String excepcioStacktrace;

	}

	/**
	 * Resposta de l'acció {@link #ACTION_PIPELLES_CODE}: les pestanyes del llistat, una per
	 * integració i en l'ordre en què s'han de mostrar.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class PipellesResult implements Serializable {

		private static final long serialVersionUID = 1L;

		private List<Pipella> pipelles;

	}

	/** Pestanya del llistat: la integració, el seu nom traduït i les peticions amb error. */
	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Pipella implements Serializable {

		private static final long serialVersionUID = 1L;

		/** Codi de la integració, el mateix que {@link IntegracioCodiEnum} i que la named query del llistat. */
		private String codi;
		private String nom;
		/** Peticions amb error de la integració (0 si no en té; {@code null} si no s'ha pogut comptar). */
		private Long errors;

	}

}
