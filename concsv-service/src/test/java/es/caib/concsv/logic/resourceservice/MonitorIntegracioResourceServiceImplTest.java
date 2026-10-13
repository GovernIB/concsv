package es.caib.concsv.logic.resourceservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.junit.Test;

import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource;
import es.caib.concsv.persist.entity.MonitorIntegracioEntity;

/**
 * Tests del servei del monitor d'integracions.
 *
 * <p>El mapeig automàtic entitat-recurs no copia els paràmetres de la petició (un mapa desat com a
 * JSON), així que se'n comprova la còpia explícita.</p>
 */
public class MonitorIntegracioResourceServiceImplTest {

	private final MonitorIntegracioResourceServiceImpl service = new MonitorIntegracioResourceServiceImpl();

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

}
