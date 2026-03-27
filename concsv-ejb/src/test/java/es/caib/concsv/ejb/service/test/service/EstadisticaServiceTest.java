package es.caib.concsv.ejb.service.test.service;

import es.caib.concsv.ejb.helpers.EstadisticaHelper;
import es.caib.concsv.ejb.services.EstadisticaService;
import es.caib.concsv.service.enums.ResultTypeEnum;
import es.caib.comanda.model.v1.estadistica.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EstadisticaServiceTest {

    @InjectMocks
    private EstadisticaService estadisticaService;

    @Mock
    private EstadisticaHelper estadisticaHelper;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testRegistrarPeticio_LlamaHelper() {
        String origen = "TEST";
        ResultTypeEnum result = ResultTypeEnum.OK;
        long duration = 123L;

        estadisticaService.registrarPeticio(origen, result, duration);

        verify(estadisticaHelper, times(1)).register(origen, result, duration);
    }

    @Test
    public void testFlushEstadisticas_LlamaHelper() {
        LocalDate date = LocalDate.now();

        estadisticaService.flushEstadisticas(date);

        verify(estadisticaHelper, times(1)).flushToDatabase(date);
    }

    @Test
    public void testNetejarEstadistiques_LlamaHelper() {
        LocalDate date = LocalDate.now();

        estadisticaService.netejarEstadistiques(date);

        verify(estadisticaHelper, times(1)).netejarEstadistiques(date);
    }

    @Test
    public void testConsultaUltimesEstadistiques_UsaHelper() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(estadisticaHelper.findFirstTempsByData(yesterday)).thenReturn(java.util.Optional.empty());

        RegistresEstadistics result = estadisticaService.consultaUltimesEstadistiques();

        assertNotNull(result);
        verify(estadisticaHelper, times(1)).findFirstTempsByData(yesterday);
    }

    @Test
    public void testConsultaEstadistiquesRango_LlamaHelperPorCadaDia() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end   = LocalDate.of(2025, 1, 3);

        when(estadisticaHelper.findFirstTempsByData(any())).thenReturn(java.util.Optional.empty());

        List<RegistresEstadistics> results = estadisticaService.consultaEstadistiques(start, end);

        assertEquals(3, results.size());
        verify(estadisticaHelper, times(3)).findFirstTempsByData(any());
    }

    @Test
    public void testGetDimensions_RetornaCorrectamente() {
        List<DimensioDesc> dimensions = estadisticaService.getDimensions();

        assertNotNull(dimensions);
        assertEquals(2, dimensions.size());
        assertTrue(dimensions.stream().anyMatch(d -> d.getCodi().equals("TIP")));
        assertTrue(dimensions.stream().anyMatch(d -> d.getCodi().equals("ORI")));
    }

    @Test
    public void testGetIndicadors_RetornaVacio() {
        List<IndicadorDesc> indicators = estadisticaService.getIndicadors();
        assertNotNull(indicators);
        assertFalse(indicators.isEmpty());
    }
}
