package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceArtifact;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.annotation.ResourceField;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.model.FileReference;
import es.caib.concsv.logic.intf.base.model.ResourceArtifactType;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.resourcevalidation.ValidImageFile;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Informació d'una entitat.
 * <p>
 * És el mateix concepte d'entitat que a DISTRIBUCIO, però sense permisos: a ConCSV tots els
 * superusuaris veuen totes les entitats i la resta d'usuaris només les poden consultar (per a
 * triar-ne una al selector de la capçalera).
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = EntitatResource.Fields.nom,
		quickFilterFields = { EntitatResource.Fields.codi, EntitatResource.Fields.nom, EntitatResource.Fields.cif },
		accessConstraints = {
				// CREATE i DELETE són permisos independents de WRITE (veure
				// PermissionEvaluatorService.toBasePermissions): sense declarar-los el motor
				// genèric no ofereix ni el botó de crear de la graella ni l'acció d'esborrar de
				// la fila, encara que l'usuari pugui modificar el recurs.
				@ResourceAccessConstraint(
						type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
						roles = { BaseConfig.ROLE_SUPER },
						grantedPermissions = {
								PermissionEnum.READ,
								PermissionEnum.WRITE,
								PermissionEnum.CREATE,
								PermissionEnum.DELETE }
				),
				// Només lectura per a la resta d'usuaris: la necessita el selector d'entitat de la
				// capçalera (veure ConcsvProvider).
				@ResourceAccessConstraint(
						type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
						roles = { BaseConfig.ROLE_USER },
						grantedPermissions = { PermissionEnum.READ }
				)
		},
		artifacts = {
				@ResourceArtifact(
						type = ResourceArtifactType.FILTER,
						code = EntitatResource.FILTER_CODE,
						formClass = EntitatResource.FormFilter.class),
				// Activació i desactivació de l'entitat. Sense formClass no obren cap formulari:
				// s'executen directament sobre la fila. Sense accessConstraints pròpies requereixen
				// el permís WRITE sobre el recurs, és a dir, CSV_SUPER.
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = EntitatResource.ACTION_ACTIVAR_CODE,
						requiresId = true),
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = EntitatResource.ACTION_DESACTIVAR_CODE,
						requiresId = true)
		}
)
public class EntitatResource extends BaseResource<Long> {

	public static final String FILTER_CODE = "FILTER";
	public static final String ACTION_ACTIVAR_CODE = "ACTIVAR";
	public static final String ACTION_DESACTIVAR_CODE = "DESACTIVAR";

	/** Colors en hexadecimal CSS (#rrggbb), el format que retorna el selector de color del navegador. */
	private static final String COLOR_HEX = "^#[0-9a-fA-F]{6}$";
	private static final String COLOR_HEX_MESSAGE = "{es.caib.concsv.validation.constraints.ColorHex}";

	@NotNull
	@Size(max = 64)
	private String codi;
	@NotNull
	@Size(max = 256)
	private String nom;
	@Size(max = 1024)
	private String descripcio;
	@NotNull
	@Size(max = 9)
	private String cif;
	@NotNull
	@Size(max = 9)
	private String codiDir3;
	@Size(max = 32)
	@Pattern(regexp = COLOR_HEX, message = COLOR_HEX_MESSAGE)
	private String colorFons;
	@Size(max = 32)
	@Pattern(regexp = COLOR_HEX, message = COLOR_HEX_MESSAGE)
	private String colorLletra;
	private byte[] logoImgBytes;
	@Size(max = 32)
	@Pattern(regexp = COLOR_HEX, message = COLOR_HEX_MESSAGE)
	private String colorFonsDark;
	@Size(max = 32)
	@Pattern(regexp = COLOR_HEX, message = COLOR_HEX_MESSAGE)
	private String colorLletraDark;
	private byte[] logoImgBytesDark;

	@Transient
	@ResourceField(onChangeActive = true)
	@ValidImageFile
	private FileReference logoImgFile;

	@Transient
	@ResourceField(onChangeActive = true)
	@ValidImageFile
	private FileReference logoImgFileDark;

	private LocalDateTime fechaActualizacion;
	private LocalDateTime fechaSincronizacion;

	/** Per defecte cert: les entitats sempre es creen actives. */
	private boolean activa = true;

	/**
	 * Camps del filtre del llistat d'entitats.
	 * <p>
	 * {@code activa} és un {@link Boolean} (no un {@code boolean}) perquè el motor genèric de
	 * recursos el representi amb un desplegable de tres valors -- buit, Sí i No -- i així es
	 * puguin consultar tant les actives com les inactives.
	 */
	@Getter
	@Setter
	public static class FormFilter implements Serializable {

		private static final long serialVersionUID = 1L;

		private String codi;
		private String nom;
		private String cif;
		private String codiDir3;
		private Boolean activa;

	}

}
