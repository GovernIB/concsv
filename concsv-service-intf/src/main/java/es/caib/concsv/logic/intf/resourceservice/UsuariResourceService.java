package es.caib.concsv.logic.intf.resourceservice;

import es.caib.concsv.logic.intf.base.service.MutableResourceService;
import es.caib.concsv.logic.intf.model.UsuariResource;

/**
 * Definició del servei de consulta i modificació del perfil de l'usuari autenticat actual.
 *
 * @author Límit Tecnologies
 */
public interface UsuariResourceService extends MutableResourceService<UsuariResource, String> {

	/**
	 * Dona d'alta l'usuari autenticat, si encara no existeix, i n'actualitza les dades personals
	 * (nom, NIF i correu) amb les del token d'autenticació.
	 */
	void refresh();

}
