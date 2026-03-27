package es.caib.concsv.ejb.scheduler;
import es.caib.concsv.service.facade.EstadisticaServiceInterface;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.time.LocalDate;

@Slf4j
@Singleton
@Startup
public class EstadisticaTimer {

    @EJB private EstadisticaServiceInterface estadisticaService;

    @Schedule(hour = "*", minute = "*/5", second = "0", persistent = false)
    public void actualizarInformacionEstadistica() {
        try {
            estadisticaService.flushEstadisticas(LocalDate.now());
        } catch (Exception e) {
            log.error("Error en actualitzar estadístiques", e);
        }
    }

    @Schedule(hour = "2", minute = "30", second = "0", persistent = false)
    public void netejarInformacionEstadistica() {
        try {
            estadisticaService.netejarEstadistiques(LocalDate.now());
        } catch (Exception e) {
            log.error("Error en netejar estadístiques", e);
        }
    }
}