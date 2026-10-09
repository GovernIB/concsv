package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceArtifact;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.model.FileReference;
import es.caib.concsv.logic.intf.base.model.ResourceArtifactType;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.List;

/**
 * Document exclòs de la descàrrega de l'original, identificat pel seu UUID o CSV d'Arxiu.
 * <p>
 * El manteniment és només del superusuari del backoffice. La llista és global i, a més del CRUD, 
 * es pot exportar i importar amb el fitxer de text {@code documents-exclosos.txt} (un valor per línia) i esborrar en bloc.
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = DocumentExclosResource.Fields.valor,
		quickFilterFields = { DocumentExclosResource.Fields.valor },
		accessConstraints = {
				@ResourceAccessConstraint(
						type = ResourceAccessConstraint.ResourceAccessConstraintType.ROLE,
						roles = { BaseConfig.ROLE_SUPER },
						grantedPermissions = {
								PermissionEnum.READ,
								PermissionEnum.WRITE,
								PermissionEnum.CREATE,
								PermissionEnum.DELETE }
				)
		},
		artifacts = {
				// Descàrrega de tots els valors en un fitxer de text.
				@ResourceArtifact(
						type = ResourceArtifactType.REPORT,
						code = DocumentExclosResource.REPORT_EXPORTAR_CODE,
						requiresId = false),
				// Càrrega dels valors d'un fitxer de text.
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = DocumentExclosResource.ACTION_IMPORTAR_CODE,
						requiresId = false,
						formClass = DocumentExclosResource.FormImportar.class),
				// Eliminació de les files seleccionades.
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = DocumentExclosResource.ACTION_ELIMINAR_MASSIU_CODE,
						requiresId = false,
						formClass = DocumentExclosResource.FormEliminarMassiu.class)
		}
)
public class DocumentExclosResource extends BaseResource<Long> {

	public static final String REPORT_EXPORTAR_CODE = "EXPORTAR";
	public static final String ACTION_IMPORTAR_CODE = "IMPORTAR";
	public static final String ACTION_ELIMINAR_MASSIU_CODE = "ELIMINAR_MASSIU";

	/** Longitud màxima del valor (columna {@code VALOR}). */
	public static final int VALOR_MAX = 256;

	/** UUID o CSV del document. */
	@NotNull
	@Size(min = 1, max = VALOR_MAX)
	private String valor;

	/**
	 * Formulari de la importació: fitxer de text amb un valor per línia.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@FieldNameConstants
	public static class FormImportar implements Serializable {
		private static final long serialVersionUID = 1L;
		@NotNull
		private FileReference fitxer;
	}

	/**
	 * Resultat de la importació: valors afegits, valors que ja hi eren o estaven repetits al
	 * fitxer, i línies descartades per ser massa llargues.
	 */
	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ResultatImportacio implements Serializable {
		private static final long serialVersionUID = 1L;
		private int afegits;
		private int jaExistents;
		private int descartats;
	}

	/**
	 * Formulari de l'eliminació en bloc.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@FieldNameConstants
	public static class FormEliminarMassiu implements Serializable {
		private static final long serialVersionUID = 1L;
		@NotNull
		private List<Long> ids;
	}

}
