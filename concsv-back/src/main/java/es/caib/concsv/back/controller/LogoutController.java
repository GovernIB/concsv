package es.caib.concsv.back.controller;

import es.caib.concsv.back.config.WebSecurityConfig;
import es.caib.concsv.back.helper.OidcDiscoveryHelper;
import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakSecurityContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Tancament de sessió en mode JBoss: invalida la sessió HTTP local i redirigeix el navegador a
 * l'"end session endpoint" de l'IdP, que és qui tanca la sessió SSO i n'esborra les galetes. En
 * mode Spring Boot aquesta URL la resol abans el filtre de logout de Spring Security (veure
 * {@link WebSecurityConfig}), que fa el logout OIDC.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Controller
public class LogoutController {

	@GetMapping(WebSecurityConfig.LOGOUT_URL)
	public String logout(HttpServletRequest request) {
		// Cal llegir el context de Keycloak ABANS d'invalidar la sessió.
		KeycloakSecurityContext keycloakSecurityContext = getKeycloakSecurityContext(request);
		// NO cridis request.logout(): amb l'adaptador Keycloak fa un logout backchannel que no
		// passa pel navegador, així que les galetes SSO de l'IdP no s'esborren i l'usuari torna a
		// entrar silenciosament; a més, el redirect de sota rebria "Session not active".
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}
		String postLogoutUrl = ServletUriComponentsBuilder.fromContextPath(request).
				path(BaseConfig.REACT_APP_PATH + "/").
				build().
				toUriString();
		if (keycloakSecurityContext == null || keycloakSecurityContext.getIdToken() == null) {
			log.warn("Tancament de sessió sense context de Keycloak: no es pot tancar la sessió SSO");
			return "redirect:" + postLogoutUrl;
		}
		// L'issuer i el client es llegeixen del mateix id_token (claims "iss" i "azp") i no de la
		// configuració: així no es poden desincronitzar del que ha creat realment la sessió SSO.
		String issuerUrl = keycloakSecurityContext.getIdToken().getIssuer();
		String endSessionEndpoint = OidcDiscoveryHelper.getEndSessionEndpoint(issuerUrl);
		if (endSessionEndpoint == null) {
			endSessionEndpoint = issuerUrl + "/protocol/openid-connect/logout";
		}
		UriComponentsBuilder logoutUrl = UriComponentsBuilder.fromHttpUrl(endSessionEndpoint).
				queryParam("post_logout_redirect_uri", "{postLogoutUrl}").
				queryParam("id_token_hint", "{idToken}");
		String clientId = keycloakSecurityContext.getIdToken().getIssuedFor();
		if (clientId != null) {
			logoutUrl.queryParam("client_id", "{clientId}");
		}
		return "redirect:" + logoutUrl.
				encode().
				buildAndExpand(postLogoutUrl, keycloakSecurityContext.getIdTokenString(), clientId).
				toUriString();
	}

	private KeycloakSecurityContext getKeycloakSecurityContext(HttpServletRequest request) {
		// No es pot llegir de request.getUserPrincipal(): Spring Security embolcalla la petició i
		// hi retorna la seva Authentication en lloc del KeycloakPrincipal del contenidor. L'adaptador
		// Keycloak deixa el context també com a atribut de la petició.
		Object attribute = request.getAttribute(KeycloakSecurityContext.class.getName());
		if (attribute instanceof KeycloakSecurityContext) {
			return (KeycloakSecurityContext)attribute;
		}
		return null;
	}

}
