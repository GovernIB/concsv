package es.caib.concsv.ejb.service.test.helper;

import es.caib.comanda.model.v1.salut.*;
import es.caib.concsv.ejb.helpers.SubsistemesHelper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SubsistemesHelperTest {

	private SubsistemesHelper helper;

	@Before
	public void setUp() {
		helper = new SubsistemesHelper();
		helper.setRegistry(new SimpleMeterRegistry());
		helper.init();
	}

	@Test
	public void testAddSuccessOperationAfectaSalut() {
		long start = System.currentTimeMillis();
		helper.addOperation(SubsistemesHelper.SubsistemesEnum.CHE, start, false);
		SubsistemesHelper.SubsistemesInfo info = helper.getSubsistemesInfo();
		assertNotNull(info);
		assertNotNull(info.getSubsistemesSalut());

		SubsistemaSalut che = info.getSubsistemesSalut().stream()
			.filter(s -> s.getCodi().equals(SubsistemesHelper.SubsistemesEnum.CHE.name()))
			.findFirst().orElse(null);

		assertNotNull(che);
		assertEquals(Long.valueOf(1L), che.getTotalOk());
		assertEquals(Long.valueOf(0L), che.getTotalError());
		assertTrue(che.getEstat() == EstatSalutEnum.UP || che.getEstat() == EstatSalutEnum.WARN);
	}

	@Test
	public void testAddErrorOperationAfectaSalut() {
		helper.addOperation(SubsistemesHelper.SubsistemesEnum.ORI, System.currentTimeMillis(), true);
		helper.addOperation(SubsistemesHelper.SubsistemesEnum.ORI, System.currentTimeMillis(), true);
		helper.addOperation(SubsistemesHelper.SubsistemesEnum.ORI, System.currentTimeMillis(), true);
		SubsistemesHelper.SubsistemesInfo info = helper.getSubsistemesInfo();
		assertNotNull(info);

		SubsistemaSalut ori = info.getSubsistemesSalut().stream()
			.filter(s -> s.getCodi().equals(SubsistemesHelper.SubsistemesEnum.ORI.name()))
			.findFirst().orElse(null);

		assertNotNull(ori);
		assertEquals(Long.valueOf(0L), ori.getTotalOk());
		assertTrue(ori.getTotalError() > 0);
		assertEquals(EstatSalutEnum.DOWN, ori.getEstat());
	}

	@Test
	public void testEstatGlobalSeCalculaCorrectamente() {
		helper.addOperation(SubsistemesHelper.SubsistemesEnum.CHE, System.currentTimeMillis(), false);
		for (int i = 0; i < 3; i++) {
			helper.addOperation(SubsistemesHelper.SubsistemesEnum.ORI, System.currentTimeMillis(), true);
		}

		SubsistemesHelper.SubsistemesInfo info = helper.getSubsistemesInfo();
		assertEquals(EstatSalutEnum.DOWN, info.getEstatGlobal());
	}
}
