package es.caib.concsv.ejb.service.test.helper;

import es.caib.concsv.ejb.helpers.EstadisticaHelper;
import es.caib.concsv.persistence.entity.ExplotDimensioEntity;
import es.caib.concsv.persistence.entity.ExplotFetsEntity;
import es.caib.concsv.persistence.entity.ExplotTempsEntity;
import es.caib.concsv.persistence.model.EnviamentOrigen;
import es.caib.concsv.persistence.model.EnviamentTipus;
import es.caib.concsv.service.enums.ResultTypeEnum;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EstadisticaHelperTest {

    @InjectMocks
    private EstadisticaHelper helper;

    @Mock
    private EntityManager entityManager;
    private AutoCloseable closeable;

    @Before
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        System.setProperty("es.caib.concsv.estadisticas.dias.conservar", "365");
    }

    @Test
    public void testRegisterOkIncrementsCounterAndTime() {
        helper.register("IMPRIMIBLE:API", ResultTypeEnum.OK, 120L);

        assertTrue(helper.getCounters().containsKey("IMPRIMIBLE:API"));
        assertEquals(1, helper.getCounters().get("IMPRIMIBLE:API").get(ResultTypeEnum.OK).longValue());
        assertEquals(120L, helper.getTimeAccumulators().get("IMPRIMIBLE:API").longValue());
    }

    @Test
    public void testRegisterErrorIncrementsCounterWithoutTime() {
        helper.register("IMPRIMIBLE:API", ResultTypeEnum.ERROR, 200L);

        assertTrue(helper.getCounters().containsKey("IMPRIMIBLE:API"));
        assertEquals(1, helper.getCounters().get("IMPRIMIBLE:API").get(ResultTypeEnum.ERROR).longValue());
        assertFalse(helper.getTimeAccumulators().containsKey("IMPRIMIBLE:API"));
    }

    @Test
    public void testFlushToDatabaseCreatesNewEntity() {
        LocalDate today = LocalDate.now();
        helper.register("IMPRIMIBLE:API", ResultTypeEnum.OK, 100L);

        ExplotTempsEntity temps = new ExplotTempsEntity(today);
        ExplotDimensioEntity dim = new ExplotDimensioEntity();

        EstadisticaHelper spyHelper = Mockito.spy(helper);

        doReturn(Optional.of(temps)).when(spyHelper).findFirstTempsByData(today);
        doReturn(Optional.of(dim)).when(spyHelper).findDimensioByTipusAndOrigen(EnviamentTipus.IMPRIMIBLE, EnviamentOrigen.API);
        doReturn(Optional.empty()).when(spyHelper).findFetByDimensioAndTemps(dim, temps);
        doReturn(mock(ExplotFetsEntity.class)).when(spyHelper).save(any(ExplotFetsEntity.class));

        spyHelper.flushToDatabase(today);

        verify(spyHelper, times(1)).save(argThat((ExplotFetsEntity entity) ->
                entity.getCorrecte() == 1 &&
                        entity.getError() == 0 &&
                        entity.getTempsMigCorrecte() == 100L
        ));

        assertEquals(0, spyHelper.getCounters().get("IMPRIMIBLE:API").get(ResultTypeEnum.OK).longValue());
        assertFalse(spyHelper.getTimeAccumulators().containsKey("IMPRIMIBLE:API"));
    }

    @Test
    public void testFlushToDatabaseUpdatesExistingEntity() {
        LocalDate today = LocalDate.now();
        helper.register("IMPRIMIBLE:API", ResultTypeEnum.OK, 200L);

        ExplotTempsEntity temps = new ExplotTempsEntity(today);
        ExplotDimensioEntity dim = new ExplotDimensioEntity();
        ExplotFetsEntity existing = ExplotFetsEntity.builder()
                .dimensio(dim)
                .temps(temps)
                .correcte(2L)
                .error(1L)
                .codiInvalid(0L)
                .tempsMigCorrecte(50L)
                .build();

        EstadisticaHelper spyHelper = Mockito.spy(helper);

        doReturn(Optional.of(temps)).when(spyHelper).findFirstTempsByData(today);
        doReturn(Optional.of(dim)).when(spyHelper).findDimensioByTipusAndOrigen(EnviamentTipus.IMPRIMIBLE, EnviamentOrigen.API);
        doReturn(Optional.of(existing)).when(spyHelper).findFetByDimensioAndTemps(dim, temps);
        doReturn(existing).when(spyHelper).save(existing);

        spyHelper.flushToDatabase(today);

        assertEquals(3L, existing.getCorrecte().longValue());
        assertEquals(1L, existing.getError().longValue());
        assertTrue(existing.getTempsMigCorrecte() > 50L);
    }

    @Test
    public void testNetejarEstadistiquesDeletesOldTempsAndFets() {
        LocalDate today = LocalDate.of(2025, 10, 21);
        LocalDate fechaLimite = today.minusDays(helper.getDiasConservar());
        ExplotTempsEntity old1 = new ExplotTempsEntity(fechaLimite.minusDays(1));
        ExplotTempsEntity old2 = new ExplotTempsEntity(fechaLimite.minusDays(2));

        EstadisticaHelper spyHelper = Mockito.spy(helper);
        doReturn(List.of(old1, old2)).when(spyHelper).findAllTempsBefore(fechaLimite);
        doNothing().when(spyHelper).deleteFetsByTemps(any());

        spyHelper.netejarEstadistiques(today);

        verify(spyHelper, times(1)).findAllTempsBefore(fechaLimite);
        verify(spyHelper, times(1)).deleteFetsByTemps(old1);
        verify(spyHelper, times(1)).deleteFetsByTemps(old2);
        verify(entityManager, times(1)).remove(old1);
        verify(entityManager, times(1)).remove(old2);
    }
}
