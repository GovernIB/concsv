package es.caib.concsv.logic.helper;

import es.caib.concsv.persist.entity.ConfigEntity;
import es.caib.concsv.persist.repository.ConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.ConfigProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Accés a les propietats configurables de l'aplicació que es mantenen des del backoffice.
 * <p>
 * El valor efectiu d'una propietat global és:
 * <ul>
 * <li>{@code jbossProperty}: el del fitxer de propietats del servidor (la mateixa font que els
 * {@code @ConfigProperty} dels serveis);</li>
 * <li>editable: el valor de la base de dades i, si és nul, el del fitxer.</li>
 * </ul>
 * Si el fitxer no la defineix, el valor és nul i l'aplicació usa el valor per defecte del codi. Els
 * serveis CDI apliquen la mateixa regla amb {@link ConfigValues}.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigHelper {

	private final ConfigRepository configRepository;
	private final Environment springEnvironment;

	/**
	 * Valor d'una propietat definida al fitxer de propietats del servidor.
	 * <p>
	 * Es consulta MicroProfile Config, que és on es carreguen els fitxers
	 * {@code es.caib.concsv.properties} i {@code es.caib.concsv.system.properties}. Si no està
	 * disponible (per exemple en mode standalone, on el {@code PropertyFileConfigSource} falla si
	 * no hi ha les propietats de sistema) es consulta l'entorn de Spring.
	 *
	 * @param key
	 *            la clau de la propietat.
	 * @param defaultValue
	 *            el valor a retornar si la propietat no està definida.
	 * @return el valor de la propietat o el valor per defecte.
	 */
	public String getEnvironmentProperty(String key, String defaultValue) {
		String value = null;
		try {
			Optional<String> mpValue = ConfigProvider.getConfig().getOptionalValue(key, String.class);
			value = mpValue.orElse(null);
		} catch (Throwable ex) {
			log.debug("No s'ha pogut llegir la propietat {} amb MicroProfile Config: {}", key, ex.toString());
		}
		if (value == null) {
			value = springEnvironment.getProperty(key);
		}
		return value != null ? value : defaultValue;
	}

	/** Valor efectiu d'una fila de propietat (veure la documentació de la classe). */
	public String valorEfectiu(ConfigEntity config) {
		if (config.isJbossProperty() || config.getValue() == null) {
			return getEnvironmentProperty(config.getKey(), null);
		}
		return config.getValue();
	}

	/** Valor efectiu d'una propietat. */
	@Transactional(readOnly = true)
	public String getConfig(String key) {
		if (key == null) {
			return null;
		}
		Optional<ConfigEntity> config = configRepository.findById(key);
		if (!config.isPresent()) {
			// Propietat que només és al fitxer de propietats
			return getEnvironmentProperty(key, null);
		}
		return valorEfectiu(config.get());
	}

	/**
	 * Copia a la base de dades el valor que el fitxer de propietats del servidor defineix per a cada
	 * propietat editable. Les propietats que el fitxer no defineix no es toquen.
	 *
	 * @return el nombre de propietats actualitzades.
	 */
	@Transactional
	public int synchronize() {
		int actualitzades = 0;
		for (ConfigEntity config : configRepository.findByJbossPropertyFalse()) {
			String value = getEnvironmentProperty(config.getKey(), null);
			if (value != null) {
				config.setValue(value);
				actualitzades++;
			}
		}
		log.info("Sincronització de propietats: {} actualitzades", actualitzades);
		return actualitzades;
	}

}
