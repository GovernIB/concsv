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

import org.springframework.data.annotation.Transient;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
				// Filtre del llistat (UUID, nom, interval de la data de captura i estat).
				@ResourceArtifact(
						type = ResourceArtifactType.FILTER,
						code = DocumentExclosResource.FILTER_CODE,
						formClass = DocumentExclosResource.FormFilter.class),
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
						formClass = DocumentExclosResource.FormEliminarMassiu.class),
				// Nova consulta de les dades del document (nom, data de captura...) d'una fila.
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = DocumentExclosResource.ACTION_ACTUALITZAR_CODE,
						requiresId = true),
				// Nova consulta de les dades del document de les files seleccionades.
				@ResourceArtifact(
						type = ResourceArtifactType.ACTION,
						code = DocumentExclosResource.ACTION_ACTUALITZAR_MASSIU_CODE,
						requiresId = false,
						formClass = DocumentExclosResource.FormActualitzarMassiu.class)
		}
)
public class DocumentExclosResource extends BaseResource<Long> {

	public static final String FILTER_CODE = "FILTER";
	public static final String REPORT_EXPORTAR_CODE = "EXPORTAR";
	public static final String ACTION_IMPORTAR_CODE = "IMPORTAR";
	public static final String ACTION_ELIMINAR_MASSIU_CODE = "ELIMINAR_MASSIU";
	public static final String ACTION_ACTUALITZAR_CODE = "ACTUALITZAR";
	public static final String ACTION_ACTUALITZAR_MASSIU_CODE = "ACTUALITZAR_MASSIU";

	/** Longitud màxima del valor (columna {@code VALOR}). */
	public static final int VALOR_MAX = 256;

	/**
	 * Nombre màxim d'identificadors d'una sola petició d'actualització massiva: cada document és una
	 * consulta a l'Arxiu i l'acció s'executa dins una transacció amb temps límit. El front parteix la
	 * selecció en lots d'aquesta mida.
	 */
	public static final int ACTUALITZAR_MASSIU_MAX = 10;

	/** UUID o CSV del document. */
	@NotNull
	@Size(min = 1, max = VALOR_MAX)
	private String valor;

	// Dades del document. Les omple sempre el servidor, consultant el document: el que
	// enviï un client es sobreescriu en crear i en modificar.

	/** Nom del document. */
	private String nomDocument;
	/** Data de captura del document. */
	private LocalDateTime dataCaptura;
	/** CSV del document (el del valor, o el resolt a partir de l'UUID). */
	private String csv;
	/** UUID del document a l'Arxiu (el del valor, o el resolt a partir del CSV). */
	private String uuid;
	/** Òrgan del document (metadada ENI). */
	private String organ;
	/** Origen del document (metadada ENI): ciutadà o administració. */
	private DocumentExclosOrigenEnum origen;
	/** On s'ha trobat el document: a l'Arxiu o a la custòdia antiga. */
	private DocumentExclosUbicacioEnum ubicacio;
	/** Data de la darrera consulta de les dades del document. */
	private LocalDateTime dataActualitzacio;
	/** Estat de les dades: resultat de la darrera consulta, o {@code PENDENT} si encara no s'ha consultat. */
	private DocumentExclosEstatEnum estatActualitzacio = DocumentExclosEstatEnum.PENDENT;
	/** Motiu del no trobat o de l'error. */
	private String missatgeActualitzacio;
	/** Enllaç a la consulta del document al front públic; es calcula, no es persisteix. */
	@Transient
	private String enllac;

	/**
	 * Formulari del filtre del llistat. Només el consumeix el front, que en construeix l'expressió.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@FieldNameConstants
	public static class FormFilter implements Serializable {
		private static final long serialVersionUID = 1L;
		/** UUID (o CSV) del document, o part. */
		private String uuid;
		private String nomDocument;
		private LocalDate dataCapturaInici;
		private LocalDate dataCapturaFi;
		private DocumentExclosEstatEnum estat;
	}

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

	/**
	 * Formulari de l'actualització de les dades del document en bloc: com a màxim
	 * {@link #ACTUALITZAR_MASSIU_MAX} identificadors per petició.
	 */
	@Getter
	@Setter
	@NoArgsConstructor
	@FieldNameConstants
	public static class FormActualitzarMassiu implements Serializable {
		private static final long serialVersionUID = 1L;
		@NotNull
		private List<Long> ids;
	}

	/**
	 * Resultat de l'actualització en bloc: documents consultats correctament, no trobats, amb error
	 * i ids que no s'han pogut processar dins el temps màxim de la petició (el client els ha de
	 * tornar a enviar).
	 */
	@Getter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ResultatActualitzacio implements Serializable {
		private static final long serialVersionUID = 1L;
		private int actualitzats;
		private int noTrobats;
		private int errors;
		private List<Long> pendents;
	}

}
