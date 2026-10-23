package es.caib.concsv.back.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import es.caib.concsv.back.base.config.BaseWebMvcConfig;
import es.caib.concsv.logic.intf.base.util.RequestSessionUtil;
import es.caib.concsv.logic.intf.base.util.ThreadLocalUtil;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.UserSession;
import es.caib.concsv.logic.intf.resourceservice.UsuariResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.AsyncHandlerInterceptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Configuració de Spring MVC.
 *
 * @author Límit Tecnologies
 */
@Configuration
@Order
@DependsOn("ejbClientConfig")
public class WebMvcConfig extends BaseWebMvcConfig {

	/**
	 * Orígens des d'on es pot cridar l'API amb credencials: el servidor de desenvolupament de Vite
	 * ({@code npm run dev}) i el mateix backend.
	 */
	public static final String[] ORIGENS_CORS = new String[] {
			"http://localhost:5173",
			"http://localhost:8080"
	};

	@Value("${" + BaseConfig.PROP_USER_SESSION_HTTP_HEADER + ":X-App-Session}")
	private String userSessionHttpHeader;

	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UsuariResourceService usuariResourceService;

	@Override
	protected boolean isJsAppResourceHandlerEnabled() {
		// L'SPA la serveix ReactController (que també retorna l'index.html per a les rutes del router).
		return false;
	}

	@Override
	protected String getJsAppStaticFolder() {
		return BaseConfig.REACT_APP_PATH;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**").
				allowedOrigins(ORIGENS_CORS).
				allowCredentials(true).
				allowedHeaders("*").
				allowedMethods("*");
	}

	/**
	 * Dona d'alta l'usuari autenticat a {@code csv_usuari} (o n'actualitza les dades personals)
	 * abans d'atendre cap petició de l'API, de manera que el seu perfil sempre existeix quan l'SPA
	 * el consulta.
	 */
	@Bean
	public HandlerInterceptor userInterceptor() {
		return new AsyncHandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
				Authentication auth = SecurityContextHolder.getContext().getAuthentication();
				if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
					usuariResourceService.refresh();
				}
				return true;
			}
		};
	}

	/**
	 * Desa l'entitat de treball de la capçalera {@code X-App-Session} en un ThreadLocal
	 * ({@link RequestSessionUtil}). Els fils del servidor es reutilitzen, així que s'ha de buidar
	 * sempre: si no, una petició sense la capçalera (el superusuari no l'envia mai) heretaria
	 * l'entitat de la darrera petició que havia servit el mateix fil.
	 */
	@Bean
	public HandlerInterceptor userSessionInterceptor() {
		return new AsyncHandlerInterceptor() {
			@Override
			public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
				ThreadLocalUtil.clear();
				String json = request.getHeader(userSessionHttpHeader);
				if (json != null) {
					Map<?, ?> parsedJson = objectMapper.readValue(json, Map.class);
					Object entitatId = parsedJson.get("e");
					Long entitatLong = entitatId instanceof Number ? ((Number)entitatId).longValue() : null;
					RequestSessionUtil.setRequestSession(new UserSession(entitatLong));
				}
				return true;
			}
			@Override
			public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
				ThreadLocalUtil.clear();
			}
			@Override
			public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
				ThreadLocalUtil.clear();
			}
		};
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(userSessionInterceptor());
		registry.addInterceptor(userInterceptor()).addPathPatterns(BaseConfig.API_PATH + "/**");
	}

}
