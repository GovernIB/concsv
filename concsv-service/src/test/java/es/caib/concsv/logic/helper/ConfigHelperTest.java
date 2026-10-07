package es.caib.concsv.logic.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;

import es.caib.concsv.persist.entity.ConfigEntity;
import es.caib.concsv.persist.repository.ConfigRepository;

/**
 * Tests de l'accés a les propietats configurables.
 *
 * <p>Es comprova la regla del valor efectiu (fitxer del servidor i propietat editable) i la
 * sincronització amb el fitxer.</p>
 */
public class ConfigHelperTest {

	private static final String KEY = "es.caib.concsv.cache.ttl.minuts";

	private ConfigRepository configRepository;
	private Environment environment;
	private ConfigHelper configHelper;

	@Before
	public void setUp() {
		configRepository = mock(ConfigRepository.class);
		environment = mock(Environment.class);
		configHelper = new ConfigHelper(configRepository, environment);
	}

	private ConfigEntity config(String key, String value, boolean jbossProperty) {
		ConfigEntity config = new ConfigEntity();
		config.setKey(key);
		config.setValue(value);
		config.setJbossProperty(jbossProperty);
		return config;
	}

	@Test
	public void environmentProperty_senseMicroProfileLlegeixDeSpring() {
		when(environment.getProperty("clau.test")).thenReturn("valor");
		assertEquals("valor", configHelper.getEnvironmentProperty("clau.test", "defecte"));
	}

	@Test
	public void environmentProperty_senseValorRetornaElPerDefecte() {
		assertEquals("defecte", configHelper.getEnvironmentProperty("clau.inexistent", "defecte"));
	}

	@Test
	public void valorEfectiu_propietatDelFitxerPrenElFitxerIMaiElValorDeLaBd() {
		ConfigEntity config = config(KEY, "30", true);
		when(environment.getProperty(KEY)).thenReturn("60");
		assertEquals("60", configHelper.valorEfectiu(config));
		when(environment.getProperty(KEY)).thenReturn(null);
		assertNull(configHelper.valorEfectiu(config));
	}

	@Test
	public void valorEfectiu_editablePrenElValorDeLaBd() {
		ConfigEntity config = config(KEY, "45", false);
		when(environment.getProperty(KEY)).thenReturn("30");
		assertEquals("45", configHelper.valorEfectiu(config));
	}

	@Test
	public void valorEfectiu_editableSenseValorPrenElFitxer() {
		ConfigEntity config = config(KEY, null, false);
		when(environment.getProperty(KEY)).thenReturn("30");
		assertEquals("30", configHelper.valorEfectiu(config));
		when(environment.getProperty(KEY)).thenReturn(null);
		assertNull(configHelper.valorEfectiu(config));
	}

	@Test
	public void getConfig_propietatEditable() {
		when(configRepository.findById(KEY)).thenReturn(Optional.of(config(KEY, "45", false)));
		assertEquals("45", configHelper.getConfig(KEY));
	}

	@Test
	public void getConfig_propietatNoRegistradaLlegeixElFitxer() {
		when(configRepository.findById("clau.fitxer")).thenReturn(Optional.empty());
		when(environment.getProperty("clau.fitxer")).thenReturn("x");
		assertEquals("x", configHelper.getConfig("clau.fitxer"));
		assertNull(configHelper.getConfig(null));
	}

	@Test
	public void synchronize_copiaElFitxerNomesALesEditablesQueEllDefineix() {
		ConfigEntity ambFitxer = config(KEY, "5", false);
		ConfigEntity senseFitxer = config("es.caib.concsv.forceValideCert", null, false);
		when(configRepository.findByJbossPropertyFalse()).thenReturn(Arrays.asList(ambFitxer, senseFitxer));
		when(environment.getProperty(KEY)).thenReturn("30");

		int actualitzades = configHelper.synchronize();

		assertEquals(1, actualitzades);
		assertEquals("30", ambFitxer.getValue());
		// La que el fitxer no defineix no es toca
		assertNull(senseFitxer.getValue());
	}

}
