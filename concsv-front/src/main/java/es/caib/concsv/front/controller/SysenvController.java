/**
 *
 */
package es.caib.concsv.front.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Servei principal de l'API de l'aplicació.
 *
 * @author Limit Tecnologies
 */
//@Hidden
@RestController
@RequestMapping(path = "/sysenv")
public class SysenvController {

	@Autowired
	private Environment env;

	private Map<String, String> additionalReactEnvVars = Map.of(
		PropertyConfig.PROP_FRONT_API_URL, "REACT_APP_API_URL",
		PropertyConfig.PROP_FRONT_PREVIEW_ENABLED, "REACT_APP_PREVIEW_ENABLED",
		PropertyConfig.PROP_FRONT_RECAPTCHA_ENABLED, "REACT_APP_RECAPTCHA_ENABLED",
		PropertyConfig.PROP_FRONT_RECAPTCHA_SITEKEY, "REACT_APP_RECAPTCHA_SITE_KEY");

	private Map<String, String> additionalViteEnvVars = Map.of(
		PropertyConfig.PROP_FRONT_API_URL, "VITE_API_URL",
		PropertyConfig.PROP_FRONT_PREVIEW_ENABLED, "VITE_PREVIEW_ENABLED",
		PropertyConfig.PROP_FRONT_RECAPTCHA_ENABLED, "VITE_RECAPTCHA_ENABLED",
		PropertyConfig.PROP_FRONT_RECAPTCHA_SITEKEY, "VITE_RECAPTCHA_SITE_KEY");

	@GetMapping
	public ResponseEntity<String> systemEnvironment(
			@RequestParam(required = false) String format) {
		Map<String, Object> systemEnv = getAllProperties(env); // System.getenv();
		MediaType contentType = MediaType.TEXT_PLAIN;
		String envJson;
		if ("reactapp".equalsIgnoreCase(format)) {
			String json = systemEnv.entrySet().stream().
					filter(e -> e.getKey().startsWith("REACT_APP") || additionalReactEnvVars.keySet().contains(e.getKey())).
					map(e -> {
						if (e.getKey().startsWith("REACT_APP")) {
							return "\"" + e.getKey() + "\":\"" + e.getValue() + "\",";
						} else {
							return "\"" + additionalReactEnvVars.get(e.getKey()) + "\":\"" + e.getValue() + "\",";
						}
					}).
					collect(Collectors.joining("\n"));
			envJson = "window.__RUNTIME_CONFIG__ = {" + json + "}";
			contentType = MediaType.valueOf("text/javascript");
		} else if ("vite".equalsIgnoreCase(format)) {
			String json = systemEnv.entrySet().stream().
					filter(e -> e.getKey().startsWith("VITE") || additionalViteEnvVars.keySet().contains(e.getKey())).
					map(e -> {
						if (e.getKey().startsWith("VITE")) {
							return "\"" + e.getKey() + "\":\"" + e.getValue() + "\",";
						} else {
							return "\"" + additionalViteEnvVars.get(e.getKey()) + "\":\"" + e.getValue() + "\",";
						}
					}).
					collect(Collectors.joining("\n"));
			envJson = "window.__RUNTIME_CONFIG__ = {" + json + "}";
			contentType = MediaType.valueOf("text/javascript");
		} else {
			envJson = "";
		}
		return ResponseEntity.
				ok().
				contentType(contentType).
				body(envJson);
	}

	@SuppressWarnings("rawtypes")
	public static Map<String, Object> getAllProperties(Environment env) {
		Map<String, Object> props = new HashMap<>();
		if (env instanceof ConfigurableEnvironment) {
			for (PropertySource<?> propertySource : ((ConfigurableEnvironment) env).getPropertySources()) {
				if (propertySource instanceof EnumerablePropertySource) {
					for (String key: ((EnumerablePropertySource)propertySource).getPropertyNames()) {
						props.put(key, propertySource.getProperty(key));
					}
				}
			}
		}
		return props;
	}

}
