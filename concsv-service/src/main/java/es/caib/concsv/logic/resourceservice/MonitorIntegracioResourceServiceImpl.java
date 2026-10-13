package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource;
import es.caib.concsv.logic.intf.resourceservice.MonitorIntegracioResourceService;
import es.caib.concsv.persist.entity.MonitorIntegracioEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * Implementació del servei de consulta del monitor d'integracions.
 *
 * @author Límit Tecnologies
 */
@Service
public class MonitorIntegracioResourceServiceImpl
		extends BaseMutableResourceService<MonitorIntegracioResource, Long, MonitorIntegracioEntity>
		implements MonitorIntegracioResourceService {

	/**
	 * El mapeig automàtic no copia els paràmetres (són un mapa desat com a JSON), així que es lliuren aquí.
	 */
	@Override
	protected void afterConversion(MonitorIntegracioEntity entity, MonitorIntegracioResource resource) {
		resource.setParametres(entity.getParametres());
	}

	/**
	 * Cada pestanya del llistat és una "named query" amb el codi de la integració: 
	 * filtra per aquest codi sense que el client l'hagi d'enviar dins el filtre. 
	 * Un codi que no existeix no retorna cap fila (mai totes).
	 */
	@Override
	protected <P> Specification<P> namedQueryToSpecification(String namedQuery) {
		return (root, query, cb) -> {
			try {
				return cb.equal(root.get("codi"), IntegracioCodiEnum.valueOf(namedQuery));
			} catch (IllegalArgumentException | NullPointerException ex) {
				return cb.disjunction();
			}
		};
	}

}
