package es.caib.concsv.ejb.services;

import es.caib.comanda.model.v1.estadistica.*;
import es.caib.concsv.ejb.annotation.ErrorInt;
import es.caib.concsv.ejb.annotation.PerformanceInt;
import es.caib.concsv.ejb.estadistiques.DimensioEnum;
import es.caib.concsv.ejb.estadistiques.DimensioConcsv;
import es.caib.concsv.ejb.estadistiques.FetEnum;
import es.caib.concsv.ejb.estadistiques.FetConcsv;
import es.caib.concsv.ejb.helpers.EstadisticaHelper;
import es.caib.concsv.persistence.entity.ExplotDimensioEntity;
import es.caib.concsv.persistence.entity.ExplotFetsEntity;
import es.caib.concsv.persistence.entity.ExplotTempsEntity;
import es.caib.concsv.persistence.model.*;
import es.caib.concsv.service.enums.ResultTypeEnum;
import es.caib.concsv.service.facade.EstadisticaServiceInterface;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;


@ErrorInt
@PerformanceInt
@Stateless @Local(EstadisticaServiceInterface.class)
@Slf4j
public class EstadisticaService implements EstadisticaServiceInterface {

    @Inject private EstadisticaHelper estadisticaHelper;

    @PostConstruct
    public void init() {}

    /** Registra una petició en memòria (OK/INVALID/ERROR) */
    @Override
    @PermitAll
    public void registrarPeticio(String origen, ResultTypeEnum result, long durationMillis) {
        estadisticaHelper.register(origen, result, durationMillis);
    }

    /** Persisteix les dades acumulades a la base de dades */
    @Override
    @PermitAll
    public void flushEstadisticas(LocalDate date) {
        estadisticaHelper.flushToDatabase(date);
    }

    /** Esborra tots els Temps i Fets anteriors a la data límit */
    @Override
    @PermitAll
    public void netejarEstadistiques(LocalDate date) {
        estadisticaHelper.netejarEstadistiques(date);
    }

    @Override
    @PermitAll
    public RegistresEstadistics consultaUltimesEstadistiques() {
        return consultaEstadistiques(ahir());
    }

    @Override
    @PermitAll
    public RegistresEstadistics consultaEstadistiques(LocalDate data) {
        return getRegistresEstadistics(data);
    }

    @Override
    @PermitAll
    public List<RegistresEstadistics> consultaEstadistiques(LocalDate dataInici, LocalDate dataFi) {
        List<RegistresEstadistics> result = new ArrayList<>();
        LocalDate currentDate = dataInici;
        while (!currentDate.isAfter(dataFi)) {
            result.add(getRegistresEstadistics(currentDate));
            currentDate = currentDate.plusDays(1);
        }
        return result;
    }

    @Override
    @PermitAll
    public List<DimensioDesc> getDimensions() {
        List<String> tipus = Arrays.stream(EnviamentTipus.values()).map(Enum::name).sorted().collect(Collectors.toList());
        List<String> origens = Arrays.stream(EnviamentOrigen.values()).map(Enum::name).sorted().collect(Collectors.toList());

        return List.of(
                DimensioDesc.builder().codi(DimensioEnum.TIP.name()).nom(DimensioEnum.TIP.getNom()).descripcio(DimensioEnum.TIP.getDescripcio()).valors(tipus).build(),
                DimensioDesc.builder().codi(DimensioEnum.ORI.name()).nom(DimensioEnum.ORI.getNom()).descripcio(DimensioEnum.ORI.getDescripcio()).valors(origens).build()
        );
    }

    @Override
    @PermitAll
    public List<IndicadorDesc> getIndicadors() {
        return Arrays.stream(FetEnum.values())
                .map(fet -> IndicadorDesc.builder()
                        .codi(fet.name())
                        .nom(fet.getNom())
                        .descripcio(fet.getDescripcio())
                        .format(Format.LONG)
                        .build()).collect(Collectors.toList());
    }

    private RegistresEstadistics getRegistresEstadistics(LocalDate data) {
        ExplotTempsEntity temps = estadisticaHelper.findFirstTempsByData(data).orElse(null);
        if (temps == null) {
            Date dia = Date.from(data.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
            return RegistresEstadistics.builder()
                    .temps(Temps.builder().data(dia).build())
                    .fets(List.of())
                    .build();
        }

        List<ExplotFetsEntity> fets = estadisticaHelper.findFetByTemps(temps);

        return toRegistresEstadistics(fets, data);
    }

    private RegistresEstadistics toRegistresEstadistics(List<ExplotFetsEntity> fets, LocalDate data) {
        Date dia = Date.from(data.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
        return RegistresEstadistics.builder()
                .temps(Temps.builder().data(dia).build())
                .fets(fets.stream().map(this::toRegistreEstadistic).collect(Collectors.toList()))
                .build();
    }

    private RegistreEstadistic toRegistreEstadistic(ExplotFetsEntity fet) {
        return RegistreEstadistic.builder()
                .dimensions(toDimensions(fet.getDimensio()))
                .fets(toFets(fet))
                .build();
    }

    private List<Dimensio> toDimensions(ExplotDimensioEntity dimensio) {
        return List.of(
                new DimensioConcsv(DimensioEnum.TIP, dimensio.getTipus()),
                new DimensioConcsv(DimensioEnum.ORI, dimensio.getOrigen())
        );
    }

    private List<Fet> toFets(ExplotFetsEntity fet) {
        return List.of(
                new FetConcsv(FetEnum.CORRECTE, fet.getCorrecte()),
                new FetConcsv(FetEnum.CODI_INVALID, fet.getCodiInvalid()),
                new FetConcsv(FetEnum.ERROR, fet.getError()),
                new FetConcsv(FetEnum.TEMPS_MITJ_CORRECTE, fet.getTempsMigCorrecte())
        );
    }

    private LocalDate ahir() {
        return LocalDate.now().minusDays(1);
    }

}
