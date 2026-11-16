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

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Document de la cache de documents del servidor (veure {@link CacheCarpeta}). És un recurs de només
 * lectura que no s'emmagatzema a base de dades: cada fila és un fitxer de la carpeta
 * {@code <es.caib.concsv.fitxers>/cache/<carpeta>}, i només és visible pel superusuari.
 * <p>
 * L'identificador del recurs és {@code <CARPETA>:<nom del fitxer>}. La data de darrera consulta és
 * la de darrera modificació del fitxer (la cache la renova a cada lectura) i la d'expiració s'obté
 * sumant-hi el temps de vida configurat (propietat {@code cache.ttl.minuts}).
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = CacheDocumentResource.Fields.identificador,
		quickFilterFields = { CacheDocumentResource.Fields.identificador },
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
				roles = { BaseConfig.ROLE_SUPER },
				grantedPermissions = { PermissionEnum.READ }
		),
		artifacts = {
				@ResourceArtifact(
						type = ResourceArtifactType.FILTER,
						code = CacheDocumentResource.FILTER_CODE,
						formClass = CacheDocumentResource.FormFilter.class),
				// Esborra les entrades seleccionades. Sense WRITE al recurs, que és de només lectura:
				// el control d'accés és el de l'acció (només CSV_SUPER).
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = CacheDocumentResource.ACTION_ESBORRAR_MASSIU_CODE,
						requiresId = false,
						formClass = CacheDocumentResource.FormEsborrarMassiu.class,
						accessConstraints = @ResourceAccessConstraint(
								type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
								roles = { BaseConfig.ROLE_SUPER },
								grantedPermissions = { PermissionEnum.READ }
						))
		}
)
public class CacheDocumentResource extends BaseResource<String> {

	public static final String FILTER_CODE = "FILTER";
	public static final String ACTION_ESBORRAR_MASSIU_CODE = "ESBORRAR_MASSIU";

	/** Carpeta de la cache on és el fitxer. */
	private CacheCarpeta carpeta;
	/**
	 * UUID o CSV del document, tal com es desa al nom del fitxer: sense el prefix {@code UUID_} o
	 * {@code CSV_} ni l'idioma. Els caràcters que no són lletres, xifres, {@code .}, {@code _} o
	 * {@code -} hi apareixen com a {@code _}.
	 */
	private String identificador;
	/** {@code UUID} o {@code CSV}: només a la carpeta {@link CacheCarpeta#INFO}. */
	private String tipusIdentificador;
	/** Idioma del document: només a la carpeta {@link CacheCarpeta#IMPRIMIBLE}. */
	private String idioma;
	/** Nom del fitxer dins la carpeta. */
	private String nomFitxer;
	/**
	 * Nom real del document, llegit de l'objecte {@code DocumentInfo} serialitzat. Només a la
	 * carpeta {@link CacheCarpeta#INFO}: a {@code ORIGINAL} i {@code IMPRIMIBLE} l'objecte
	 * serialitzat ({@code DocumentContent}) inclou el contingut binari del document, i llegir-lo
	 * només per obtenir-ne el nom seria massa costós amb molts fitxers (veure
	 * {@code CacheDocumentHelper}). {@code null} si no s'ha pogut llegir (fitxer corrupte o no
	 * serialitzat).
	 */
	private String nomDocument;
	/** Mida del fitxer en bytes. */
	private Long mida;
	/** Darrera vegada que s'ha consultat l'entrada (darrera modificació del fitxer). */
	private Date darreraConsulta;
	/** Moment en què l'entrada expira; {@code null} si la cache no té temps de vida. */
	private Date expiracio;

	/**
	 * Formulari del filtre del llistat.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@FieldNameConstants
	public static class FormFilter implements Serializable {
		private static final long serialVersionUID = 1L;
		private CacheCarpeta carpeta;
		private String identificador;
		private Boolean caducat;
	}

	/**
	 * Formulari de l'esborrat de les entrades seleccionades.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@FieldNameConstants
	public static class FormEsborrarMassiu implements Serializable {
		private static final long serialVersionUID = 1L;
		@NotNull
		private List<String> ids;
	}

	/**
	 * Resultat de l'esborrat: fitxers esborrats i fitxers que no s'han pogut esborrar.
	 */
	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ResultatEsborrat implements Serializable {
		private static final long serialVersionUID = 1L;
		private int esborrats;
		private int errors;
	}

}
