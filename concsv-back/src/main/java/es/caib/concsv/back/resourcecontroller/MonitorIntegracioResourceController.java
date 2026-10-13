package es.caib.concsv.back.resourcecontroller;

import es.caib.concsv.back.base.controller.BaseMutableResourceController;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servei REST de consulta del monitor d'integracions.
 *
 * @author Límit Tecnologies
 */
@RestController
@RequestMapping(BaseConfig.API_PATH + "/monitorIntegracio")
public class MonitorIntegracioResourceController extends BaseMutableResourceController<MonitorIntegracioResource, Long> {

}
