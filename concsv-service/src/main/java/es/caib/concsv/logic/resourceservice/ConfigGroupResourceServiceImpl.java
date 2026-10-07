package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.intf.model.ConfigGroupResource;
import es.caib.concsv.logic.intf.resourceservice.ConfigGroupResourceService;
import es.caib.concsv.persist.entity.ConfigGroupEntity;
import org.springframework.stereotype.Service;

/**
 * Implementació del servei de gestió de grups de propietats configurables.
 *
 * @author Límit Tecnologies
 */
@Service
public class ConfigGroupResourceServiceImpl extends BaseMutableResourceService<ConfigGroupResource, String, ConfigGroupEntity> implements ConfigGroupResourceService {}
