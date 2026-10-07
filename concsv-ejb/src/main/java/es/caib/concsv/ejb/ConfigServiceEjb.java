package es.caib.concsv.ejb;

import es.caib.concsv.logic.intf.qualifier.LogicService;
import es.caib.concsv.logic.intf.service.ConfigServiceInterface;
import lombok.experimental.Delegate;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Local
@Stateless
public class ConfigServiceEjb implements ConfigServiceInterface {

	@Inject
	@LogicService
	@Delegate
	private ConfigServiceInterface delegate;

}
