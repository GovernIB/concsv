package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.helper.AuthenticationHelper;
import es.caib.concsv.logic.helper.ExecucioMassivaCuaHelper;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaResource;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.entity.UsuariEntity;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.UsuariRepository;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Proves de {@link ExecucioMassivaResourceServiceImpl}.
 */
public class ExecucioMassivaResourceServiceImplTest {

	private ExecucioMassivaContingutRepository contingutRepository;
	private UsuariRepository usuariRepository;
	private ExecucioMassivaResourceServiceImpl service;

	@Before
	public void setUp() {
		contingutRepository = mock(ExecucioMassivaContingutRepository.class);
		usuariRepository = mock(UsuariRepository.class);
		service = new ExecucioMassivaResourceServiceImpl(
				contingutRepository, mock(ExecucioMassivaCuaHelper.class), mock(AuthenticationHelper.class), usuariRepository);
	}

	private static ExecucioMassivaEntity execucio(ExecucioMassivaEstatEnum estat) {
		ExecucioMassivaEntity execucio = new ExecucioMassivaEntity();
		execucio.setId(1L);
		execucio.setTipus(ExecucioMassivaTipusEnum.ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS);
		execucio.setEstat(estat);
		return execucio;
	}

	private static Object[] fila(ExecucioMassivaContingutEstatEnum estat, long n) {
		return new Object[] { estat, n };
	}

	private static UsuariEntity usuari(String codi, String nom) {
		UsuariEntity usuari = new UsuariEntity();
		usuari.setId(codi);
		usuari.setNom(nom);
		return usuari;
	}

	@Test
	public void elCreadorEsMostraComNomIdentificador() {
		when(usuariRepository.findById("csv_super")).thenReturn(Optional.of(usuari("csv_super", "UsuariCSV Superadmin")));

		assertEquals("UsuariCSV Superadmin (csv_super)", service.nomComplet("csv_super"));
	}

	@Test
	public void senseNomOSensePerfilEsMostraNomesElCodi() {
		when(usuariRepository.findById("sense_nom")).thenReturn(Optional.of(usuari("sense_nom", "  ")));
		when(usuariRepository.findById("desconegut")).thenReturn(Optional.empty());

		assertEquals("sense_nom", service.nomComplet("sense_nom"));
		assertEquals("desconegut", service.nomComplet("desconegut"));
		assertEquals(null, service.nomComplet(null));
	}

	@Test
	public void afterConversionOmpleElCreadorAmbElNomComplet() {
		when(contingutRepository.countPerEstat(1L)).thenReturn(Collections.emptyList());
		when(usuariRepository.findById("csv_super")).thenReturn(Optional.of(usuari("csv_super", "UsuariCSV Superadmin")));
		ExecucioMassivaEntity entity = execucio(ExecucioMassivaEstatEnum.PENDENT);
		entity.setCreatedBy("csv_super");
		ExecucioMassivaResource resource = new ExecucioMassivaResource();

		service.afterConversion(entity, resource);

		assertEquals("UsuariCSV Superadmin (csv_super)", resource.getCreatedByFullName());
	}

	@Test
	public void afterConversionCalculaElsRecomptesPerEstat() {
		when(contingutRepository.countPerEstat(1L)).thenReturn(Arrays.asList(
				fila(ExecucioMassivaContingutEstatEnum.FINALITZAT, 7),
				fila(ExecucioMassivaContingutEstatEnum.ERROR, 2),
				fila(ExecucioMassivaContingutEstatEnum.PENDENT, 4),
				fila(ExecucioMassivaContingutEstatEnum.CANCELAT, 1)));
		ExecucioMassivaResource resource = new ExecucioMassivaResource();

		service.afterConversion(execucio(ExecucioMassivaEstatEnum.EN_CURS), resource);

		assertEquals(7, resource.getFinalitzats());
		assertEquals(2, resource.getErrors());
		assertEquals(4, resource.getPendents());
		assertEquals(1, resource.getCancelats());
		assertEquals(14, resource.getTotal());
	}

	@Test
	public void afterConversionSenseElementsDeixaTotsElsRecomptesAZero() {
		when(contingutRepository.countPerEstat(1L)).thenReturn(Collections.emptyList());
		ExecucioMassivaResource resource = new ExecucioMassivaResource();

		service.afterConversion(execucio(ExecucioMassivaEstatEnum.PENDENT), resource);

		assertEquals(0, resource.getTotal());
		assertEquals(0, resource.getErrors());
	}

	@Test
	public void unaExecucioPendentOEnCursSePotCancelar() {
		assertTrue(ExecucioMassivaResourceServiceImpl.esCancelable(execucio(ExecucioMassivaEstatEnum.PENDENT)));
		assertTrue(ExecucioMassivaResourceServiceImpl.esCancelable(execucio(ExecucioMassivaEstatEnum.EN_CURS)));
	}

	@Test
	public void unaExecucioAcabadaNoSePotCancelar() {
		ExecucioMassivaEntity acabada = execucio(ExecucioMassivaEstatEnum.FINALITZADA);
		acabada.setDataFi(LocalDateTime.now());
		assertFalse(ExecucioMassivaResourceServiceImpl.esCancelable(acabada));
	}

	@Test
	public void unaExecucioAmbLaCancelacioJaDemanadaNoSePotTornarACancelar() {
		ExecucioMassivaEntity enCurs = execucio(ExecucioMassivaEstatEnum.EN_CURS);
		enCurs.setCancelacioData(LocalDateTime.now());
		assertFalse(ExecucioMassivaResourceServiceImpl.esCancelable(enCurs));
	}

	@Test
	public void afterConversionMarcaCancelableSegonsLEstat() {
		when(contingutRepository.countPerEstat(1L)).thenReturn(Collections.emptyList());
		ExecucioMassivaResource enCurs = new ExecucioMassivaResource();
		ExecucioMassivaResource cancelada = new ExecucioMassivaResource();
		ExecucioMassivaEntity entitatCancelada = execucio(ExecucioMassivaEstatEnum.CANCELADA);
		entitatCancelada.setDataFi(LocalDateTime.now());

		service.afterConversion(execucio(ExecucioMassivaEstatEnum.EN_CURS), enCurs);
		service.afterConversion(entitatCancelada, cancelada);

		assertTrue(enCurs.isCancelable());
		assertFalse(cancelada.isCancelable());
	}

}
