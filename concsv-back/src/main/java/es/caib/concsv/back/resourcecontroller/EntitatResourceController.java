package es.caib.concsv.back.resourcecontroller;

import es.caib.concsv.back.base.controller.BaseMutableResourceController;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.EntitatResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servei REST de gestió d'entitats.
 *
 * @author Límit Tecnologies
 */
@RestController
@RequestMapping(BaseConfig.API_PATH + "/entitats")
public class EntitatResourceController extends BaseMutableResourceController<EntitatResource, Long> {

}
