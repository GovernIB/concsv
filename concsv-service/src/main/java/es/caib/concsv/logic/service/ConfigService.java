package es.caib.concsv.logic.service;

import es.caib.concsv.logic.annotation.ErrorInt;
import es.caib.concsv.logic.annotation.PerformanceInt;
import es.caib.concsv.logic.helper.ConfigValues;
import es.caib.concsv.logic.intf.qualifier.LogicService;
import es.caib.concsv.logic.intf.service.ConfigServiceInterface;

import javax.annotation.security.PermitAll;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/** Accés de les aplicacions web (el front públic) a les propietats configurables. */
@ErrorInt
@PerformanceInt
@LogicService
@ApplicationScoped
public class ConfigService implements ConfigServiceInterface {

	@Inject
	private ConfigValues configValues;

	@PermitAll
	@Override
	public String getProperty(String key) {
		return configValues.get(key);
	}

}
