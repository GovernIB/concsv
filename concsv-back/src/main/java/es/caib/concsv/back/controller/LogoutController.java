package es.caib.concsv.back.controller;

import es.caib.concsv.back.config.WebSecurityConfig;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Tancament de sessió en mode JBoss: {@code request.logout()} fa que l'adaptador Keycloak del
 * contenidor tanqui també la sessió SSO. En mode Spring Boot aquesta URL la resol abans el filtre
 * de logout de Spring Security (veure {@link WebSecurityConfig}), que fa el logout OIDC.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Controller
public class LogoutController {

	@GetMapping(WebSecurityConfig.LOGOUT_URL)
	public String logout(HttpServletRequest request) {
		try {
			request.logout();
		} catch (ServletException ex) {
			log.error("Error en el tancament de sessió", ex);
		}
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		return "redirect:" + BaseConfig.REACT_APP_PATH + "/";
	}

}
