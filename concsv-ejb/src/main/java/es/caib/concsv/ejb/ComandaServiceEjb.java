package es.caib.concsv.ejb;

import es.caib.concsv.logic.intf.service.ComandaServiceInterface;
import es.caib.concsv.logic.intf.qualifier.LogicService;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Local;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.LocalDate;

@Slf4j
@Local
@Stateless
public class ComandaServiceEjb implements ComandaServiceInterface {

	@Inject
	@LogicService
	@Delegate
	private ComandaServiceInterface delegate;

	@Schedule(hour = "*", minute = "*/5", persistent = false)
	public void actualizarInformacionEstadistica() {
		try {
			delegate.flushEstadisticas(LocalDate.now());
		} catch (Exception e) {
			log.error("Error en actualitzar estadístiques", e);
		}
	}

	@Schedule(hour = "2", minute = "30", persistent = false)
	public void netejarInformacionEstadistica() {
		try {
			delegate.netejarEstadistiques(LocalDate.now());
		} catch (Exception e) {
			log.error("Error en netejar estadístiques", e);
		}
	}

}
