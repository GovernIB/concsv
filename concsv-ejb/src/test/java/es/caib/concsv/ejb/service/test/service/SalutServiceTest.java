package es.caib.concsv.ejb.service.test.service;

import es.caib.concsv.ejb.helpers.EstadisticaHelper;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import es.caib.concsv.ejb.helpers.SubsistemesHelper;
import es.caib.concsv.ejb.helpers.SubsistemesHelper.SubsistemesInfo;
import es.caib.concsv.ejb.services.SalutService;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.comanda.model.v1.salut.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;


/** Prova la logica de SalutService. */
public class SalutServiceTest {

	@InjectMocks private SalutService salutService;
	@Mock private SubsistemesHelper subsistemesHelper;
	@Mock private IntegracionsHelper integracionsHelper;
    @Mock private EstadisticaHelper estadisticaHelper;

	@Before
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	public void testAppInfo() throws GenericServiceException {
		when(integracionsHelper.getIntegracionsInfo())
			.thenReturn(List.of(IntegracioInfo.builder().codi("I1").nom("Integracio 1").build()));

		SubsistemesInfo subsInfo = mock(SubsistemesInfo.class);
		when(subsistemesHelper.getSubsistemesInfo()).thenReturn(subsInfo);
		when(subsInfo.getSubsistemesSalut())
			.thenReturn(List.of(SubsistemaSalut.builder().codi("S1").totalOk(3L).totalError(1L).build()));

		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getScheme()).thenReturn("http");
		when(request.getServerName()).thenReturn("localhost");
		when(request.getServerPort()).thenReturn(8080);

		Map<String, Object> manifestInfo = new HashMap<>();
		manifestInfo.put("Implementation-Version", "Test Version");
		manifestInfo.put("Build-Timestamp", "2024-01-01T00:00:00Z");
		manifestInfo.put("Implementation-SCM-Revision", "Rev");
		manifestInfo.put("Build-Jdk-Spec", "17");

		AppInfo info = salutService.appInfo(manifestInfo, request);

		assertEquals("CSV", info.getCodi());
		assertEquals("Test Version", info.getVersio());
		assertNotNull(info.getIntegracions());
		assertNotNull(info.getSubsistemes());
	}

	@Test
	public void testCheckSalut() throws GenericServiceException, IOException {
		when(integracionsHelper.checkIntegracions())
			.thenReturn(List.of(IntegracioSalut.builder().codi("I1").estat(EstatSalutEnum.UP).build()));
		SubsistemesInfo subsInfo = mock(SubsistemesInfo.class);
		when(subsistemesHelper.getSubsistemesInfo()).thenReturn(subsInfo);
		when(subsInfo.getSubsistemesSalut())
			.thenReturn(List.of(SubsistemaSalut.builder()
                    .codi("S1")
                    .latencia(0)
                    .estat(EstatSalutEnum.UP)
                    .totalOk(3L)
                    .totalError(1L)
                    .totalTempsMig(0)
                    .peticionsOkUltimPeriode(0L)
                    .peticionsErrorUltimPeriode(0L)
                    .tempsMigUltimPeriode(0)
                    .build()));

		SalutInfo salutInfo = salutService.checkSalut("test-version", "");

		assertEquals("CSV", salutInfo.getCodi());
		assertNotNull(salutInfo.getIntegracions());
		assertNotNull(salutInfo.getSubsistemes());
		assertEquals("test-version", salutInfo.getVersio());
	}
}
