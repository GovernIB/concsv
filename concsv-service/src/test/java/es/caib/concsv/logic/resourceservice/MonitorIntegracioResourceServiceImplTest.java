package es.caib.concsv.logic.resourceservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.junit.Test;

import es.caib.concsv.logic.helper.IntegracioDiagnosticHelper;
import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource;
import es.caib.concsv.persist.entity.MonitorIntegracioEntity;
import es.caib.concsv.persist.repository.MonitorIntegracioRepository;

/**
 * Tests del servei del monitor d'integracions.
 *
 * <p>El mapeig automàtic entitat-recurs no copia els paràmetres de la petició (un mapa desat com a
 * JSON), així que se'n comprova la còpia explícita.</p>
 */
public class MonitorIntegracioResourceServiceImplTest {

	private final MonitorIntegracioRepository repository = mock(MonitorIntegracioRepository.class);
	private final MonitorIntegracioResourceServiceImpl service = new MonitorIntegracioResourceServiceImpl(mock(IntegracioDiagnosticHelper.class), repository);

	@Test
	public void afterConversion_copiaElsParametres() {
		MonitorIntegracioEntity entity = new MonitorIntegracioEntity();
		Map<String, String> parametres = Collections.singletonMap("hash", "ab12");
		entity.setParametres(parametres);
		MonitorIntegracioResource resource = new MonitorIntegracioResource();

		service.afterConversion(entity, resource);

		assertEquals(parametres, resource.getParametres());
	}

	@Test
	public void afterConversion_senseParametresElsDeixaBuits() {
		MonitorIntegracioResource resource = new MonitorIntegracioResource();

		service.afterConversion(new MonitorIntegracioEntity(), resource);

		assertNull(resource.getParametres());
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void namedQuery_filtraPelCodiDeLaIntegracio() {
		Root<MonitorIntegracioEntity> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path codi = mock(Path.class);
		Predicate esperat = mock(Predicate.class);
		when(root.get("codi")).thenReturn(codi);
		when(cb.equal(codi, IntegracioCodiEnum.CUSTODIA)).thenReturn(esperat);

		Predicate resultat = service.<MonitorIntegracioEntity>namedQueryToSpecification("CUSTODIA")
				.toPredicate(root, query, cb);

		assertSame(esperat, resultat);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void namedQuery_ambCodiDesconegutNoRetornaCapFila() {
		Root<MonitorIntegracioEntity> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Predicate buit = mock(Predicate.class);
		when(cb.disjunction()).thenReturn(buit);

		Predicate resultat = service.<MonitorIntegracioEntity>namedQueryToSpecification("NO_EXISTEIX")
				.toPredicate(root, query, cb);

		assertSame(buit, resultat);
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void namedQuery_filtraPerLaConversioDeDocuments() {
		Root<MonitorIntegracioEntity> root = mock(Root.class);
		CriteriaQuery<?> query = mock(CriteriaQuery.class);
		CriteriaBuilder cb = mock(CriteriaBuilder.class);
		Path codi = mock(Path.class);
		Predicate esperat = mock(Predicate.class);
		when(root.get("codi")).thenReturn(codi);
		when(cb.equal(codi, IntegracioCodiEnum.CONVERSIO)).thenReturn(esperat);

		Predicate resultat = service.<MonitorIntegracioEntity>namedQueryToSpecification("CONVERSIO")
				.toPredicate(root, query, cb);

		assertSame(esperat, resultat);
	}

	@Test
	public void pipelles_retornaLesQuatreIntegracionsAmbElsErrors() {
		when(repository.countPerCodiByEstat(IntegracioAccioEstatEnum.ERROR)).thenReturn(Arrays.<Object[]>asList(
				new Object[] { IntegracioCodiEnum.ARXIU, 12L },
				new Object[] { IntegracioCodiEnum.VALIDACIO_FIRMES, 3L }));

		List<MonitorIntegracioResource.Pipella> pipelles = service.pipelles();

		assertEquals(
				Arrays.asList("ARXIU", "CUSTODIA", "VALIDACIO_FIRMES", "CONVERSIO"),
				pipelles.stream().map(MonitorIntegracioResource.Pipella::getCodi).collect(Collectors.toList()));
		assertEquals(
				Arrays.asList(12L, 0L, 3L, 0L),
				pipelles.stream().map(MonitorIntegracioResource.Pipella::getErrors).collect(Collectors.toList()));
	}

	@Test
	public void pipelles_senseErrorsLesRetornaTotesAZero() {
		when(repository.countPerCodiByEstat(IntegracioAccioEstatEnum.ERROR)).thenReturn(Collections.emptyList());

		List<MonitorIntegracioResource.Pipella> pipelles = service.pipelles();

		assertEquals(IntegracioCodiEnum.values().length, pipelles.size());
		assertTrue(pipelles.stream().allMatch(pipella -> pipella.getErrors() == 0));
	}

	@Test
	public void pipelles_elNomEsLaTraduccioIAlsenseTraduccioElCodi() {
		when(repository.countPerCodiByEstat(IntegracioAccioEstatEnum.ERROR)).thenReturn(Collections.emptyList());
		MonitorIntegracioResourceServiceImpl spy = spy(service);
		doReturn("Custòdia antiga").when(spy).nomIntegracio(IntegracioCodiEnum.CUSTODIA);

		List<MonitorIntegracioResource.Pipella> pipelles = spy.pipelles();

		// Sense context de Spring no hi ha traducció: cau al codi.
		assertEquals("ARXIU", pipelles.get(0).getNom());
		assertEquals("Custòdia antiga", pipelles.get(1).getNom());
	}

	@Test
	public void pipelles_elRecompteEsDelsErrors() {
		when(repository.countPerCodiByEstat(IntegracioAccioEstatEnum.ERROR)).thenReturn(Collections.emptyList());

		service.pipelles();

		verify(repository).countPerCodiByEstat(IntegracioAccioEstatEnum.ERROR);
	}

	@Test
	public void pipelles_siElRecompteFallaRetornaLesPestanyesSenseErrors() {
		when(repository.countPerCodiByEstat(IntegracioAccioEstatEnum.ERROR))
				.thenThrow(new IllegalStateException("ORA-00942: taula inexistent"));

		List<MonitorIntegracioResource.Pipella> pipelles = service.pipelles();

		assertEquals(IntegracioCodiEnum.values().length, pipelles.size());
		assertEquals("ARXIU", pipelles.get(0).getCodi());
		assertTrue(pipelles.stream().allMatch(pipella -> pipella.getErrors() == null));
	}

}
