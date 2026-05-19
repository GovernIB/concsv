package es.caib.concsv.logic.service;

import es.caib.comanda.model.server.monitoring.*;
import es.caib.concsv.logic.helper.EstadisticaHelper;
import es.caib.concsv.logic.helper.IntegracionsHelper;
import es.caib.concsv.logic.helper.SubsistemesHelper;
import es.caib.concsv.logic.intf.enums.ResultTypeEnum;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ComandaServiceTest {

	@InjectMocks
	private ComandaService comandaService;

	@Mock
	private EstadisticaHelper estadisticaHelper;
	@Mock
	private SubsistemesHelper subsistemesHelper;
	@Mock
	private IntegracionsHelper integracionsHelper;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testRegistrarPeticio_LlamaHelper() {
		String origen = "TEST";
		ResultTypeEnum result = ResultTypeEnum.OK;
		long duration = 123L;
		comandaService.registrarPeticio(origen, result, duration);
		verify(estadisticaHelper, times(1)).register(origen, result, duration);
	}

	@Test
	public void testFlushEstadisticas_LlamaHelper() {
		LocalDate date = LocalDate.now();
		comandaService.flushEstadisticas(date);
		verify(estadisticaHelper, times(1)).flushToDatabase(date);
	}

	@Test
	public void testNetejarEstadistiques_LlamaHelper() {
		LocalDate date = LocalDate.now();
		comandaService.netejarEstadistiques(date);
		verify(estadisticaHelper, times(1)).netejarEstadistiques(date);
	}

	@Test
	public void testConsultaUltimesEstadistiques_UsaHelper() {
		LocalDate yesterday = LocalDate.now().minusDays(1);
		when(estadisticaHelper.findFirstTempsByData(yesterday)).thenReturn(java.util.Optional.empty());
		RegistresEstadistics result = comandaService.consultaUltimesEstadistiques();
		assertNotNull(result);
		verify(estadisticaHelper, times(1)).findFirstTempsByData(yesterday);
	}

	@Test
	public void testConsultaEstadistiquesRango_LlamaHelperPorCadaDia() {
		LocalDate start = LocalDate.of(2025, 1, 1);
		LocalDate end = LocalDate.of(2025, 1, 3);
		when(estadisticaHelper.findFirstTempsByData(any())).thenReturn(java.util.Optional.empty());
		List<RegistresEstadistics> results = comandaService.consultaEstadistiques(start, end);
		assertEquals(3, results.size());
		verify(estadisticaHelper, times(3)).findFirstTempsByData(any());
	}

	@Test
	public void testGetDimensions_RetornaCorrectamente() {
		List<DimensioDesc> dimensions = comandaService.getDimensions();
		assertNotNull(dimensions);
		assertEquals(2, dimensions.size());
		assertTrue(dimensions.stream().anyMatch(d -> d.getCodi().equals("TIP")));
		assertTrue(dimensions.stream().anyMatch(d -> d.getCodi().equals("ORI")));
	}

	@Test
	public void testGetIndicadors_RetornaVacio() {
		List<IndicadorDesc> indicators = comandaService.getIndicadors();
		assertNotNull(indicators);
		assertFalse(indicators.isEmpty());
	}

	@Test
	public void testAppInfo() throws GenericServiceException {
		when(integracionsHelper.getIntegracionsInfo())
			.thenReturn(List.of(new IntegracioInfo().codi("I1").nom("Integracio 1")));
		SubsistemesHelper.SubsistemesInfo subsInfo = mock(SubsistemesHelper.SubsistemesInfo.class);
		when(subsistemesHelper.getSubsistemesInfo()).thenReturn(subsInfo);
		when(subsInfo.getSubsistemesSalut())
			.thenReturn(List.of(new SubsistemaSalut().codi("S1").totalOk(3L).totalError(1L)));
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getScheme()).thenReturn("http");
		when(request.getServerName()).thenReturn("localhost");
		when(request.getServerPort()).thenReturn(8080);
		Map<String, Object> manifestInfo = new HashMap<>();
		manifestInfo.put("Implementation-Version", "Test Version");
		manifestInfo.put("Build-Timestamp", "2024-01-01T00:00:00Z");
		manifestInfo.put("Implementation-SCM-Revision", "Rev");
		manifestInfo.put("Build-Jdk-Spec", "17");
		AppInfo info = comandaService.appInfo(manifestInfo, "");
		assertEquals("CSV", info.getCodi());
		assertEquals("Test Version", info.getVersio());
		assertNotNull(info.getIntegracions());
		assertNotNull(info.getSubsistemes());
	}

	@Test
	public void testCheckSalut() throws GenericServiceException, IOException {
		when(integracionsHelper.checkIntegracions())
			.thenReturn(List.of(new IntegracioSalut().codi("I1").estat(EstatSalutEnum.UP)));
		SubsistemesHelper.SubsistemesInfo subsInfo = mock(SubsistemesHelper.SubsistemesInfo.class);
		when(subsistemesHelper.getSubsistemesInfo()).thenReturn(subsInfo);
		when(subsInfo.getSubsistemesSalut())
			.thenReturn(List.of(new SubsistemaSalut()
				.codi("S1")
				.latencia(0)
				.estat(EstatSalutEnum.UP)
				.totalOk(3L)
				.totalError(1L)
				.totalTempsMig(0)
				.peticionsOkUltimPeriode(0L)
				.peticionsErrorUltimPeriode(0L)
				.tempsMigUltimPeriode(0)));
		SalutInfo salutInfo = comandaService.checkSalut("test-version", "");
		assertEquals("CSV", salutInfo.getCodi());
		assertNotNull(salutInfo.getIntegracions());
		assertNotNull(salutInfo.getSubsistemes());
		assertEquals("test-version", salutInfo.getVersio());
	}

}
