package es.caib.concsv.ejb;

import es.caib.concsv.logic.intf.service.NewDigitalArchiveServiceInterface;
import es.caib.concsv.logic.intf.qualifier.LogicService;
import lombok.experimental.Delegate;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

@Local
@Stateless
public class NewDigitalArchiveServiceEjb implements NewDigitalArchiveServiceInterface {

	@Inject
	@LogicService
	@Delegate
	private NewDigitalArchiveServiceInterface delegate;

}
