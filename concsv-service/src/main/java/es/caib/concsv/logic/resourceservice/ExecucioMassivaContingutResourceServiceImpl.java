package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutResource;
import es.caib.concsv.logic.intf.resourceservice.ExecucioMassivaContingutResourceService;
import es.caib.concsv.persist.entity.ExecucioMassivaContingutEntity;
import org.springframework.stereotype.Service;

/**
 * Implementació del servei de consulta dels elements de les execucions massives. És de només
 * lectura: els elements els escriuen la creació de l'execució i el procés en segon pla.
 *
 * @author Límit Tecnologies
 */
@Service
public class ExecucioMassivaContingutResourceServiceImpl
		extends BaseMutableResourceService<ExecucioMassivaContingutResource, Long, ExecucioMassivaContingutEntity>
		implements ExecucioMassivaContingutResourceService {

}
