package es.caib.concsv.logic.intf.resourceservice;

import es.caib.concsv.logic.intf.base.service.MutableResourceService;
import es.caib.concsv.logic.intf.model.AvisResource;

import java.util.List;

/**
 * Definició del servei de gestió d'avisos.
 *
 * @author Límit Tecnologies
 */
public interface AvisResourceService extends MutableResourceService<AvisResource, Long> {

	/**
	 * Retorna els avisos actius avui: els globals (sense entitat) i els de l'entitat indicada. Si
	 * no s'indica cap entitat es retornen tots.
	 * <p>
	 * Pensat per a mostrar-los al front públic (concsv-front), encara per implementar; el
	 * backoffice no els mostra.
	 *
	 * @param entitatId
	 *            entitat de la qual s'han d'incloure els avisos (pot ser null).
	 * @return la llista d'avisos actius.
	 */
	List<AvisResource> findActius(Long entitatId);

}
