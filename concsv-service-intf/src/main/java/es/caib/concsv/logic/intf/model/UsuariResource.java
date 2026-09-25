package es.caib.concsv.logic.intf.model;

import es.caib.concsv.logic.intf.base.annotation.ResourceAccessConstraint;
import es.caib.concsv.logic.intf.base.annotation.ResourceConfig;
import es.caib.concsv.logic.intf.base.annotation.ResourceField;
import es.caib.concsv.logic.intf.base.model.BaseResource;
import es.caib.concsv.logic.intf.base.permission.PermissionEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Perfil de l'usuari autenticat actual (només es pot consultar/modificar el propi -- veure
 * {@code UsuariResourceServiceImpl.additionalSpecification}).
 * <p>
 * Conté les dades personals, que es copien del token d'autenticació, i les preferències que la
 * interfície REACT recupera en iniciar sessió: el darrer rol i la darrera entitat amb què s'ha
 * operat, l'idioma i el tema.
 *
 * @author Límit Tecnologies
 */
@Getter
@Setter
@NoArgsConstructor
@FieldNameConstants
@ResourceConfig(
		descriptionField = UsuariResource.Fields.nom,
		accessConstraints = @ResourceAccessConstraint(
				type = ResourceAccessConstraint.ResourceAccessConstraintType.AUTHENTICATED,
				grantedPermissions = { PermissionEnum.READ, PermissionEnum.WRITE }
		)
)
public class UsuariResource extends BaseResource<String> {

	@Size(max = 200)
	private String nom;
	@Size(max = 9)
	private String nif;
	@Size(max = 200)
	private String email;
	/**
	 * Idioma de la interfície. Es guarda com a text amb el nom de la constant d'{@link IdiomaEnum}
	 * ("CA"/"ES"). {@code enumType} fa que el motor genèric el publiqui com a camp d'opcions, que
	 * aporta el {@code FieldOptionsProvider} registrat a {@code UsuariResourceServiceImpl}.
	 */
	@Size(max = 2)
	@ResourceField(enumType = true)
	private String idioma;
	/**
	 * Darrera entitat amb què ha operat l'usuari. Avui només la fa servir el rol {@code tothom}
	 * (el superusuari treballa sense entitat), però es desa sigui quin sigui el rol per si en el
	 * futur n'hi ha d'altres que operin dins una entitat.
	 */
	private Long entitatActualId;
	private Long numElementsPagina;
	private TemaAplicacioEnum temaAplicacio;
	@NotNull
	private MenuEstilEnum estilMenu = MenuEstilEnum.TEMA;
	/** Darrer rol amb què ha operat l'usuari: la interfície REACT el recupera en iniciar sessió i
	 *  el desa quan es canvia de rol des del selector. */
	@Size(max = 64)
	private String rolActual;
	/** Només es fixa en llegir el recurs (veure afterConversion); no es persisteix. */
	private String[] rols;

}
