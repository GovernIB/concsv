package es.caib.concsv.back.resourcecontroller;

import es.caib.concsv.back.base.controller.BaseMutableResourceController;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.UsuariResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servei REST de consulta i modificació del perfil de l'usuari autenticat actual.
 *
 * @author Límit Tecnologies
 */
@RestController
@RequestMapping(BaseConfig.API_PATH + "/usuaris")
public class UsuariResourceController extends BaseMutableResourceController<UsuariResource, String> {

}
