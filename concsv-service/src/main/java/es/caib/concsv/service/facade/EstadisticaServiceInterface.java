package es.caib.concsv.service.facade;


import es.caib.comanda.model.v1.estadistica.*;
import es.caib.concsv.service.enums.ResultTypeEnum;

import java.time.LocalDate;
import java.util.List;

public interface EstadisticaServiceInterface {

    RegistresEstadistics consultaUltimesEstadistiques();
    RegistresEstadistics consultaEstadistiques(LocalDate data);
    List<RegistresEstadistics> consultaEstadistiques(LocalDate dataInici, LocalDate dataFi);

    List<DimensioDesc> getDimensions();
    List<IndicadorDesc> getIndicadors();

    void registrarPeticio(String origen, ResultTypeEnum result, long durationMillis);
    void flushEstadisticas(LocalDate date);
    void netejarEstadistiques(LocalDate date);

}
