package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.intf.model.ConfigTypeResource;
import es.caib.concsv.logic.intf.resourceservice.ConfigTypeResourceService;
import es.caib.concsv.persist.entity.ConfigTypeEntity;
import org.springframework.stereotype.Service;

/**
 * Implementació del servei de gestió de tipus de propietats configurables.
 *
 * @author Límit Tecnologies
 */
@Service
public class ConfigTypeResourceServiceImpl extends BaseMutableResourceService<ConfigTypeResource, String, ConfigTypeEntity> implements ConfigTypeResourceService {}
