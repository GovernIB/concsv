package es.caib.concsv.back.config;

import es.caib.concsv.back.base.config.BaseWebSecurityConfig;
import es.caib.concsv.back.base.config.MethodSecurityConfig;
import es.caib.concsv.logic.intf.base.util.HttpRequestUtil;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.auth.AuthenticationDetails;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.SimpleAttributes2GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleMappableAttributesRetriever;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails;
import org.springframework.security.web.authentication.preauth.j2ee.J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Configuració de Spring Security. Suporta dos modes d'autenticació segons el tipus de
 * desplegament, seleccionats en temps d'execució mitjançant {@link #isJboss()}:
 * <ul>
 *     <li>Desplegat sobre JBoss (dins l'EAR): autenticació delegada al contenidor web
 *     (adaptador Keycloak de JBoss) i llegida via pre-autenticació J2EE.</li>
 *     <li>Standalone (Spring Boot): OAuth2 login (sessió) combinat amb OAuth2 resource
 *     server (Bearer JWT, usat pel SPA React quan es corre amb {@code npm run dev}).</li>
 * </ul>
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Configuration
public class WebSecurityConfig extends BaseWebSecurityConfig {

	public static final String LOGOUT_URL = "/usuari/logout";

	@Value("${es.caib.concsv.security.mappableRoles:" +
			BaseConfig.ROLE_SUPER + "," +
			BaseConfig.ROLE_USER + "}")
	private String mappableRoles;
	@Value("${" + BaseConfig.PROP_SECURITY_ROLE_HTTP_HEADER + ":X-App-Role}")
	private String selectedRoleHttpHeader;
	@Value("${es.caib.concsv.security.nameAttributeKey:preferred_username}")
	private String nameAttributeKey;
	@Value("${jboss.home.dir:#{null}}")
	private String jbossHomeDir;

	@Autowired(required = false)
	private ClientRegistrationRepository clientRegistrationRepository;

	@Override
	protected void customHttpSecurityConfiguration(HttpSecurity http) throws Exception {
		if (!isJboss()) {
			OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(
					clientRegistrationRepository);
			oidcLogoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");
			http.logout(lo -> lo.
					logoutRequestMatcher(new AntPathRequestMatcher(LOGOUT_URL)).
					invalidateHttpSession(true).
					clearAuthentication(true).
					deleteCookies("OAuth_Token_Request_State", "JSESSIONID").
					logoutSuccessHandler(oidcLogoutSuccessHandler));
		}
		http.authorizeHttpRequests().
				requestMatchers(publicRequestMatchers()).permitAll();
		// La interfície REACT indica a cada petició amb quin rol està operant l'usuari; el filtre
		// hi restringeix les autoritats perquè les comprovacions per rol responguin "opera amb
		// aquest rol" i no "té aquest rol" (veure RolSeleccionatFilter).
		http.addFilterBefore(
				new RolSeleccionatFilter(selectedRoleHttpHeader),
				AuthorizationFilter.class);
		if (!isJboss()) {
			http.sessionManagement(session -> session.
					sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
			// Les autoritats es creen sense prefix (veure MethodSecurityConfig.DEFAULT_ROLE_PREFIX),
			// però el wrapper de Spring Security que respon HttpServletRequest.isUserInRole() hi
			// anteposa "ROLE_" per defecte i no hi trobaria mai cap coincidència.
			http.servletApi(servletApi -> servletApi.rolePrefix(MethodSecurityConfig.DEFAULT_ROLE_PREFIX));
		}
		super.customHttpSecurityConfiguration(http);
	}

	/**
	 * Matchers públics addicionals als de {@link #internalPublicRequestMatchers()}: documentació
	 * de l'API i recursos estàtics de l'SPA (l'index.html i el JS no contenen dades i, sense
	 * sessió, s'han de poder carregar per a iniciar el login).
	 */
	protected RequestMatcher[] publicRequestMatchers() {
		return new RequestMatcher[] {
				new AntPathRequestMatcher("/swagger-ui/**"),
				new AntPathRequestMatcher("/api-docs"),
				new AntPathRequestMatcher("/api-docs/**/*"),
				new AntPathRequestMatcher(BaseConfig.REACT_APP_PATH + "/assets/**"),
				new AntPathRequestMatcher(BaseConfig.REACT_APP_PATH + "/favicon.png"),
		};
	}

	@Override
	protected boolean isWebContainerAuthActive() {
		return isJboss();
	}
	@Override
	protected boolean isOauth2ResourceServerActive() {
		return !isJboss();
	}
	@Override
	protected boolean isOauth2ResourceServerStateless() {
		// Conviu amb el login OIDC (basat en sessió): no es pot forçar STATELESS.
		return false;
	}
	@Override
	protected boolean isOidcClientActive() {
		return !isJboss();
	}

	@Override
	protected Set<String> getAllowedRoles() {
		Optional<HttpServletRequest> optionalRequest = HttpRequestUtil.getCurrentHttpRequest();
		Set<String> allowedRoles = Set.of(mappableRoles.split(","));
		if (optionalRequest.isPresent()) {
			// Si la petició HTTP conté la capçalera amb el rol seleccionat retorna únicament aquest
			// rol en la llista de rols permesos.
			String selectedRole = optionalRequest.get().getHeader(selectedRoleHttpHeader);
			if (selectedRole != null) {
				HashSet<String> editableAllowedRoles = new HashSet<>(allowedRoles);
				editableAllowedRoles.removeIf(s -> !s.equals(selectedRole));
				return editableAllowedRoles;
			}
		}
		return allowedRoles;
	}

	/**
	 * Concedeix el rol base {@link BaseConfig#ROLE_USER} ("tothom") a qualsevol usuari autenticat,
	 * abans que s'apliqui el filtre de rols permesos. No és un rol de Keycloak: és el rol amb què
	 * opera qualsevol usuari de l'aplicació.
	 */
	@Override
	protected void filterAllowedGrantedAuthorities(Set<GrantedAuthority> grantedAuthorities) {
		grantedAuthorities.add(new SimpleGrantedAuthority(BaseConfig.ROLE_USER));
		super.filterAllowedGrantedAuthorities(grantedAuthorities);
	}

	private boolean isJboss() {
		return jbossHomeDir != null;
	}

	@Override
	protected AuthenticationDetailsSource<HttpServletRequest, ?> getPreauthFilterAuthenticationDetailsSource() {
		J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource authenticationDetailsSource = new J2eeBasedPreAuthenticatedWebAuthenticationDetailsSource() {
			@Override
			public PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails buildDetails(HttpServletRequest context) {
				// El contenidor no coneix el rol "tothom" (no està declarat al web.xml), així que
				// s'afegeix aquí als rols J2EE perquè tot usuari autenticat el tengui també en
				// mode EAR (veure filterAllowedGrantedAuthorities).
				Collection<String> j2eeUserRoles = new HashSet<>(getUserRoles(context));
				j2eeUserRoles.add(BaseConfig.ROLE_USER);
				PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails result;
				if (context.getUserPrincipal() instanceof KeycloakPrincipal) {
					KeycloakPrincipal<?> keycloakPrincipal = ((KeycloakPrincipal<?>)context.getUserPrincipal());
					Set<String> roles = new HashSet<>(j2eeUserRoles);
					AccessToken.Access realmAccess = keycloakPrincipal.getKeycloakSecurityContext().getToken().getRealmAccess();
					if (realmAccess != null && realmAccess.getRoles() != null) {
						realmAccess.getRoles().stream().
								map(r -> MethodSecurityConfig.DEFAULT_ROLE_PREFIX + r).
								forEach(roles::add);
					}
					IDToken idToken = keycloakPrincipal.getKeycloakSecurityContext().getIdToken();
					// Les autoritats es construeixen senceres a posta: la restricció al rol
					// seleccionat es fa per petició a RolSeleccionatFilter.
					result = new PreauthWebAuthenticationDetails(
							context,
							j2eeUserRoles2GrantedAuthoritiesMapper.getGrantedAuthorities(roles),
							keycloakPrincipal.getKeycloakSecurityContext().getIdTokenString(),
							nameAttributeKey.equals("preferred_username") ?
									idToken.getPreferredUsername() :
									(String)idToken.getOtherClaims().get(nameAttributeKey),
							idToken.getName(),
							idToken.getEmail(),
							(String)idToken.getOtherClaims().get("nif"),
							roles.toArray(new String[0]));
				} else {
					Collection<? extends GrantedAuthority> grantedAuthorities = j2eeUserRoles2GrantedAuthoritiesMapper.
							getGrantedAuthorities(j2eeUserRoles);
					result = new PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails(
							context,
							grantedAuthorities);
				}
				log.debug("Created WebAuthenticationDetails for {} with roles {}",
						context.getUserPrincipal().getName(),
						result.getGrantedAuthorities());
				return result;
			}
		};
		SimpleMappableAttributesRetriever mappableAttributesRetriever = new SimpleMappableAttributesRetriever();
		mappableAttributesRetriever.setMappableAttributes(getAllowedRoles());
		authenticationDetailsSource.setMappableRolesRetriever(mappableAttributesRetriever);
		SimpleAttributes2GrantedAuthoritiesMapper attributes2GrantedAuthoritiesMapper = new SimpleAttributes2GrantedAuthoritiesMapper();
		attributes2GrantedAuthoritiesMapper.setAttributePrefix(MethodSecurityConfig.DEFAULT_ROLE_PREFIX);
		authenticationDetailsSource.setUserRoles2GrantedAuthoritiesMapper(attributes2GrantedAuthoritiesMapper);
		return authenticationDetailsSource;
	}

	@Getter
	public static class PreauthWebAuthenticationDetails
			extends PreAuthenticatedGrantedAuthoritiesWebAuthenticationDetails
			implements AuthenticationDetails {
		private final String jwtToken;
		private final String preferredUsername;
		private final String name;
		private final String email;
		private final String nif;
		private final String[] originalRoles;
		public PreauthWebAuthenticationDetails(
				HttpServletRequest request,
				Collection<? extends GrantedAuthority> authorities,
				String jwtToken,
				String preferredUsername,
				String name,
				String email,
				String nif,
				String[] originalRoles) {
			super(request, authorities);
			this.jwtToken = jwtToken;
			this.preferredUsername = preferredUsername;
			this.name = name;
			this.email = email;
			this.nif = nif;
			this.originalRoles = originalRoles;
		}
	}

}
