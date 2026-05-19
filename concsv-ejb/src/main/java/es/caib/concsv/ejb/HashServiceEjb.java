package es.caib.concsv.ejb;

import es.caib.concsv.logic.intf.service.HashServiceInterface;
import es.caib.concsv.logic.intf.qualifier.LogicService;
import lombok.experimental.Delegate;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Local
@Stateless
public class HashServiceEjb implements HashServiceInterface {

	@Inject
	@LogicService
	@Delegate
	private HashServiceInterface delegate;

}
