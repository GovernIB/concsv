package es.caib.concsv.logic.resourceservice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;

import es.caib.concsv.logic.helper.ConfigHelper;
import es.caib.concsv.logic.intf.base.exception.ResourceNotUpdatedException;
import es.caib.concsv.logic.intf.model.ConfigResource;
import es.caib.concsv.persist.entity.ConfigEntity;
import es.caib.concsv.persist.entity.ConfigTypeEntity;
import es.caib.concsv.persist.repository.ConfigRepository;

/**
 * Tests del servei de propietats configurables.
 *
 * <p>Es comprova que només es pot modificar el valor de les propietats editables, la validació per
 * tipus, que les credencials no es desen mai a la base de dades i que el valor que es mostra és
 * l'efectiu (fitxer del servidor o base de dades) amb les credencials sempre emmascarades.</p>
 */
public class ConfigResourceServiceImplTest {

	private static final String KEY = "es.caib.concsv.cache.ttl.minuts";

	private ConfigRepository configRepository;
	private Environment environment;
	private ConfigResourceServiceImpl service;

	@Before
	public void setUp() {
		configRepository = mock(ConfigRepository.class);
		environment = mock(Environment.class);
		ConfigHelper configHelper = new ConfigHelper(configRepository, environment);
		service = new ConfigResourceServiceImpl(configHelper);
	}

	private ConfigTypeEntity tipus(String codi) {
		ConfigTypeEntity type = new ConfigTypeEntity();
		type.setCode(codi);
		return type;
	}

	private ConfigEntity config(String key, String tipus, String value, boolean jbossProperty) {
		ConfigEntity config = new ConfigEntity();
		config.setKey(key);
		config.setType(tipus(tipus));
		config.setValue(value);
		config.setJbossProperty(jbossProperty);
		return config;
	}

	private ConfigResource resource(String key, String value) {
		ConfigResource resource = new ConfigResource();
		resource.setKey(key);
		resource.setValue(value);
		return resource;
	}

	/** Executa la validació prèvia a la modificació i retorna el valor que s'hauria desat. */
	private String valorDesat(ConfigEntity entity, String valorRebut) {
		ConfigResource resource = resource(entity.getKey(), valorRebut);
		service.beforeUpdateEntity(entity, resource, Collections.emptyMap());
		return resource.getValue();
	}

	private void assertRebutjat(ConfigEntity entity, String valorRebut, String fragmentDelMotiu) {
		try {
			valorDesat(entity, valorRebut);
			fail("S'esperava que es rebutgés el valor " + valorRebut);
		} catch (ResourceNotUpdatedException ex) {
			assertTrue(ex.getReason(), ex.getReason().contains(fragmentDelMotiu));
		}
	}

	// ---- Modificació ----

	@Test
	public void update_unaPropietatDelFitxerNoEsPotModificar() {
		assertRebutjat(config(KEY, "INT", null, true), "10", "fitxer de propietats del servidor");
	}

	@Test
	public void update_manaLaClauDeLaFilaINoLaDelCosDeLaPeticio() {
		ConfigEntity entity = config(KEY, "TEXT", null, false);
		ConfigResource resource = resource("una.altra.clau", "x");

		service.beforeUpdateEntity(entity, resource, Collections.emptyMap());

		assertEquals(KEY, resource.getKey());
	}

	@Test
	public void update_unValorBuitEquivalAQuedarSenseValor() {
		assertNull(valorDesat(config(KEY, "TEXT", "abc", false), ""));
		assertNull(valorDesat(config(KEY, "TEXT", "abc", false), "   "));
		assertNull(valorDesat(config(KEY, "INT", "5", false), null));
	}

	@Test
	public void update_validaElsEnters() {
		assertEquals("45", valorDesat(config(KEY, "INT", null, false), " 45 "));
		assertEquals("-3", valorDesat(config(KEY, "INT", null, false), "-3"));
		assertRebutjat(config(KEY, "INT", null, false), "abc", "enter");
		assertRebutjat(config(KEY, "INT", null, false), "4.5", "enter");
		assertRebutjat(config(KEY, "INT", null, false), "99999999999999999999", "enter");
	}

	@Test
	public void update_validaElsBooleans() {
		for (String valor : new String[] { "true", "false", "S", "N", "s", "n", "TRUE", "False" }) {
			assertEquals(valor, valorDesat(config(KEY, "BOOL", null, false), valor));
		}
		assertRebutjat(config(KEY, "BOOL", null, false), "SN", "true, false, S o N");
		assertRebutjat(config(KEY, "BOOL", null, false), "abc", "true, false, S o N");
	}

	@Test
	public void update_unTipusEnumeratNomesAcceptaElsSeusValors() {
		ConfigEntity entity = config(KEY, "NIVELL", null, false);
		entity.getType().setValue("BAIX,MITJA,ALT");

		assertEquals("MITJA", valorDesat(entity, "MITJA"));
		assertRebutjat(entity, "ALTISSIM", "BAIX, MITJA, ALT");
	}

	@Test
	public void update_elTextLliureNoEsValida() {
		assertEquals("qualsevol cosa", valorDesat(config(KEY, "TEXT", null, false), "qualsevol cosa"));
	}

	@Test
	public void update_lesCredencialsNoEsDesenMaiALaBd() {
		assertRebutjat(config("clau.password", "CREDENTIALS", null, false), "secret", "fitxers de propietats del servidor");
	}

	@Test
	public void update_laMascaraRetornadaPelFormulariConservaElValorDeLaFila() {
		ConfigEntity entity = config("clau.password", "CREDENTIALS", "valor-actual", false);

		assertEquals("valor-actual", valorDesat(entity, ConfigResourceServiceImpl.VALOR_EMMASCARAT));
	}

	@Test
	public void update_noTocaLesDadesDeAuditoriaDeLaFila() {
		ConfigEntity entity = config(KEY, "INT", "5", false);

		service.beforeUpdateSave(entity, resource(KEY, "5"), Collections.emptyMap());

		assertNull(entity.getLastModifiedDate());
	}

	// ---- Valor que es mostra ----

	private ConfigResource converteix(ConfigEntity entity) {
		ConfigResource resource = resource(entity.getKey(), entity.getValue());
		service.afterConversion(entity, resource);
		return resource;
	}

	@Test
	public void conversio_unaPropietatDelFitxerMostraElValorDelFitxer() {
		ConfigEntity entity = config(KEY, "INT", "30", true);
		when(environment.getProperty(KEY)).thenReturn("60");

		ConfigResource resource = converteix(entity);

		assertEquals("60", resource.getValue());
		assertNull(resource.getAlternativeValue());
	}

	@Test
	public void conversio_unaPropietatDelFitxerSenseValorNoMostraElDeLaBd() {
		ConfigEntity entity = config(KEY, "INT", "30", true);

		assertNull(converteix(entity).getValue());
	}

	@Test
	public void conversio_unaEditableAmbValorALaBdNoMostraElDelFitxer() {
		ConfigEntity entity = config(KEY, "INT", "45", false);
		when(environment.getProperty(KEY)).thenReturn("30");

		ConfigResource resource = converteix(entity);

		assertEquals("45", resource.getValue());
		assertNull(resource.getAlternativeValue());
	}

	@Test
	public void conversio_unaEditableSenseValorMostraElDelFitxerComASuggeriment() {
		ConfigEntity entity = config(KEY, "INT", null, false);
		when(environment.getProperty(KEY)).thenReturn("30");

		ConfigResource resource = converteix(entity);

		assertNull(resource.getValue());
		assertEquals("30", resource.getAlternativeValue());
	}

	@Test
	public void conversio_unaEditableIgualAlFitxerNoMostraValorAlternatiu() {
		ConfigEntity entity = config(KEY, "INT", "30", false);
		when(environment.getProperty(KEY)).thenReturn("30");

		assertNull(converteix(entity).getAlternativeValue());
	}

	@Test
	public void conversio_lesCredencialsSempreSEmmascaren() {
		ConfigEntity entity = config("clau.password", "CREDENTIALS", null, true);
		when(environment.getProperty("clau.password")).thenReturn("secret");

		ConfigResource resource = converteix(entity);

		assertEquals(ConfigResourceServiceImpl.VALOR_EMMASCARAT, resource.getValue());
	}

	@Test
	public void conversio_unaCredencialSenseValorNoMostraMascara() {
		assertNull(converteix(config("clau.password", "CREDENTIALS", null, true)).getValue());
	}

}
