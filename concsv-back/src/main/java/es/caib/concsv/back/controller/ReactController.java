package es.caib.concsv.back.controller;

import es.caib.concsv.back.base.controller.BaseUtilsController;
import es.caib.concsv.logic.intf.base.config.PropertyConfig;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Serveix l'SPA REACT i exposa els endpoints de {@link BaseUtilsController} ({@code /ping},
 * {@code /sysenv}, {@code /manifest}) que l'SPA llegeix com a {@code window.__RUNTIME_CONFIG__} i
 * {@code __MANIFEST__} a l'{@code index.html}. L'usuari i els rols no es publiquen aquí (el
 * navegador no gestiona cap token): surten de {@link SessioUsuariController}.
 * <p>
 * La versió i el commit que mostra el peu de pàgina surten de {@code /manifest}, que llegeix el
 * {@code META-INF/MANIFEST.MF} del WAR. Executant ConcsvBackBootApp des del codi font no n'hi ha
 * cap, i es fa servir {@code build-info.properties}, que Maven filtra amb les mateixes dades.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Controller
public class ReactController extends BaseUtilsController {

	@Autowired
	private ServletContext servletContext;

	private static final String BUILD_INFO = "/build-info.properties";

	/**
	 * Dades de la compilació per al peu de pàgina. Si hi ha el MANIFEST.MF del WAR (JBoss) es
	 * retorna aquest; si no (Spring Boot des del codi font), les de {@code build-info.properties}.
	 * Els valors que Maven no ha pogut substituir (p. ex. {@code @buildNumber@} si només ha
	 * compilat l'IDE) es descarten.
	 */
	@Override
	@GetMapping(BaseConfig.MANIFEST_PATH)
	public ResponseEntity<String> manifest() throws IOException {
		if (servletContext.getResource("/META-INF/MANIFEST.MF") != null) {
			return super.manifest();
		}
		Properties buildInfo = new Properties();
		try (InputStream is = getClass().getResourceAsStream(BUILD_INFO)) {
			if (is != null) {
				buildInfo.load(is);
			}
		}
		String json = buildInfo.stringPropertyNames().stream().
				filter(clau -> !buildInfo.getProperty(clau).matches("@[^@]+@")).
				sorted().
				map(clau -> "\"" + clau + "\":\"" + buildInfo.getProperty(clau).replace("\"", "\\\"") + "\"").
				collect(Collectors.joining(",\n"));
		return ResponseEntity.
				ok().
				contentType(MediaType.valueOf("text/javascript")).
				body("window.__MANIFEST__ = {\n" + json + "\n}");
	}

	/**
	 * L'arrel de l'aplicació porta a l'SPA. Hi arriba qui escriu /concsvback/ al navegador i, en
	 * mode Spring Boot, també la tornada del login i del logout (que redirigeixen a l'arrel).
	 */
	@GetMapping("/")
	public String arrel() {
		return "redirect:" + BaseConfig.REACT_APP_PATH + "/";
	}

	@RequestMapping(BaseConfig.REACT_APP_PATH + "/**")
	public ResponseEntity<?> serveReact(HttpServletRequest request, HttpServletResponse response) {
		String path = request.getRequestURI().replaceFirst(request.getContextPath(), "");
		try {
			InputStream resource = servletContext.getResourceAsStream(path);
			if (resource != null && !path.endsWith("/")) {
				String mimeType = servletContext.getMimeType(path);
				MediaType mediaType = mimeType != null ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;
				return ResponseEntity.ok().contentType(mediaType).body(new InputStreamResource(resource));
			}
			// Un recurs estàtic que no existeix és un 404; qualsevol altra ruta és de l'SPA i
			// retorna l'index.html perquè el router de REACT la resolgui.
			String uri = request.getRequestURI();
			if (uri.matches(".*\\.(js|css|ico|png|jpg|svg|woff2?|map)$")) {
				return ResponseEntity.notFound().build();
			}
			InputStream indexHtml = servletContext.getResourceAsStream(BaseConfig.REACT_APP_PATH + "/index.html");
			if (indexHtml == null) {
				// Passa quan s'arrenca des del codi font (document root src/main/webapp) sense haver
				// construït l'SPA: el perfil "front" del pom l'hi copia.
				log.error("No s'ha trobat {}/index.html al document root de l'aplicació web ({}). Cal construir l'SPA i copiar-lo a src/main/webapp{}.",
						BaseConfig.REACT_APP_PATH,
						servletContext.getRealPath("/"),
						BaseConfig.REACT_APP_PATH);
				return ResponseEntity.notFound().build();
			}
			return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(new InputStreamResource(indexHtml));
		} catch (Exception ex) {
			log.error("Error carregant recurs", ex);
			return ResponseEntity.internalServerError().body("Error carregant recurs");
		}
	}

	@Override
	protected boolean isReactAppMappedFrontProperty(String propertyName) {
		return PropertyConfig.REACT_APP_PROPS_MAP.containsKey(propertyName);
	}

	@Override
	protected String getReactAppMappedFrontProperty(String propertyName) {
		return PropertyConfig.REACT_APP_PROPS_MAP.get(propertyName);
	}

	@Override
	protected boolean isViteMappedFrontProperty(String propertyName) {
		return PropertyConfig.VITE_PROPS_MAP.containsKey(propertyName);
	}

	@Override
	protected String getViteMappedFrontProperty(String propertyName) {
		return PropertyConfig.VITE_PROPS_MAP.get(propertyName);
	}

}
