package es.caib.concsv.back.resourcecontroller;

import es.caib.concsv.back.base.controller.BaseMutableResourceController;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.AvisResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servei REST de gestió d'avisos.
 *
 * @author Límit Tecnologies
 */
@RestController
@RequestMapping(BaseConfig.API_PATH + "/avisos")
public class AvisResourceController extends BaseMutableResourceController<AvisResource, Long> {

}
