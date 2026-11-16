package es.caib.concsv.back.resourcecontroller;

import es.caib.concsv.back.base.controller.BaseMutableResourceController;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.CacheDocumentResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servei REST de consulta i esborrat dels documents de la cache de documents.
 *
 * @author Límit Tecnologies
 */
@RestController
@RequestMapping(BaseConfig.API_PATH + "/cacheDocuments")
public class CacheDocumentResourceController extends BaseMutableResourceController<CacheDocumentResource, String> {

}
