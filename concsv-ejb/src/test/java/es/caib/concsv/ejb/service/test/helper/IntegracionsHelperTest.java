package es.caib.concsv.ejb.service.test.helper;

import es.caib.comanda.model.v1.salut.*;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class IntegracionsHelperTest {

	private IntegracionsHelper helper;

	@Before
	public void setUp() {
		helper = new IntegracionsHelper();
		helper.setRegistry(new SimpleMeterRegistry());
		helper.init();
	}

	@Test
	public void testAddSuccessOperationAfectaSalut() throws InterruptedException {
		long start = System.currentTimeMillis();
		helper.addOperation(IntegracioApp.ARX, start, false);

		List<IntegracioSalut> integracions = helper.checkIntegracions();
		assertNotNull(integracions);

		IntegracioSalut arx = integracions.stream()
			.filter(i -> i.getCodi().startsWith("ARX"))
			.findFirst()
			.orElse(null);

		assertNotNull(arx);
		assertEquals(1l, arx.getPeticions().getTotalOk().longValue());
		assertEquals(0, arx.getPeticions().getTotalError().longValue());
		assertTrue(arx.getEstat() == EstatSalutEnum.UP || arx.getEstat() == EstatSalutEnum.WARN);
	}

	@Test
	public void testAddErrorOperationAfectaSalut() {
		for (int i = 0; i < 3; i++) {
			helper.addOperation(IntegracioApp.SIG, System.currentTimeMillis(), true);
		}

		List<IntegracioSalut> integracions = helper.checkIntegracions();
		assertNotNull(integracions);

		IntegracioSalut sig = integracions.stream()
			.filter(i -> i.getCodi().startsWith("SIG"))
			.findFirst()
			.orElse(null);

		assertNotNull(sig);
		assertEquals(0, sig.getPeticions().getTotalOk().longValue());
		assertTrue(sig.getPeticions().getTotalError() > 0);
		assertEquals(EstatSalutEnum.DOWN, sig.getEstat());
	}

	@Test
	public void testEstatDegradadoYWARN() {
		// Añadimos 2 errores y 1 éxito → error % = 66% → ERROR
		helper.addOperation(IntegracioApp.TIN, System.currentTimeMillis(), true);
		helper.addOperation(IntegracioApp.TIN, System.currentTimeMillis(), true);
		helper.addOperation(IntegracioApp.TIN, System.currentTimeMillis(), false);

		List<IntegracioSalut> integracions = helper.checkIntegracions();

		IntegracioSalut tin = integracions.stream()
			.filter(i -> i.getCodi().startsWith("TIN"))
			.findFirst()
			.orElse(null);

		assertNotNull(tin);
		assertEquals(1, tin.getPeticions().getTotalOk().longValue());
		assertEquals(2, tin.getPeticions().getTotalError().longValue());
		assertEquals(EstatSalutEnum.ERROR, tin.getEstat());
	}
}
