package es.caib.concsv.back.controller;

import es.caib.concsv.back.config.WebMvcConfig;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.UsuariResource;
import es.caib.concsv.logic.intf.resourceservice.UsuariResourceService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Usuari de la sessió de servidor amb què opera la interfície REACT.
 * <p>
 * La interfície no gestiona cap token al navegador: s'autentica amb la sessió HTTP (adaptador
 * Keycloak de JBoss en mode EAR, {@code oauth2Login} de Spring en mode Spring Boot), com RIPEA i
 * DISTRIBUCIO. Amb un token per pestanya, la renovació d'una pestanya invalidava la de les altres i
 * la que fallava es recarregava sencera. Aquest endpoint li dona el codi, el nom, el correu i els
 * rols de l'usuari, i li serveix per a comprovar si la sessió continua viva (veure
 * {@code ConcsvAuthProvider.tsx}).
 * <p>
 * Els rols són tots els de l'usuari, no només el seleccionat: la petició no du la capçalera del rol
 * seleccionat, així que {@link es.caib.concsv.back.config.RolSeleccionatFilter} no hi restringeix
 * res.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SessioUsuariController {

	/** Rols que la interfície REACT pot oferir al selector de rol. */
	public static final String[] ROLS_INTERFICIE = new String[] {
			BaseConfig.ROLE_SUPER,
			BaseConfig.ROLE_USER
	};

	private final UsuariResourceService usuariResourceService;

	@GetMapping(BaseConfig.API_PATH + "/sessioUsuari")
	public ResponseEntity<SessioUsuari> sessioUsuari() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		Set<String> autoritats = auth.getAuthorities().stream().
				map(GrantedAuthority::getAuthority).
				collect(Collectors.toSet());
		List<String> rols = Arrays.stream(ROLS_INTERFICIE).
				filter(autoritats::contains).
				collect(Collectors.toList());
		// El nom i el correu són només per a mostrar-los: si no es poden obtenir, la interfície
		// mostra el codi. L'usuari ja existeix a csv_usuari perquè WebMvcConfig.userInterceptor el
		// dona d'alta abans d'atendre qualsevol petició de l'API.
		String nom = null;
		String email = null;
		try {
			UsuariResource usuari = usuariResourceService.getOne(auth.getName(), null);
			nom = usuari.getNom();
			email = usuari.getEmail();
		} catch (Exception ex) {
			log.debug("No s'han pogut obtenir les dades de l'usuari {}", auth.getName(), ex);
		}
		return ResponseEntity.ok(new SessioUsuari(auth.getName(), nom, email, rols));
	}

	/**
	 * Inicia la sessió i torna a la URL indicada. Ho fa servir la interfície servida pel servidor
	 * de desenvolupament de Vite ({@code npm run dev}), que no passa pel backend.
	 * <p>
	 * No fa res per si mateix: com qualsevol altra URL de l'aplicació, requereix autenticació, així
	 * que si no hi ha sessió el servidor passa pel login i hi torna en acabar. Aleshores redirigeix
	 * a {@code retorn}. No és sota {@code /api} perquè allà, en mode Spring Boot, una petició sense
	 * sessió rep un 401 i no la redirecció al login.
	 * <p>
	 * Només es torna a URLs del mateix backend o dels orígens permesos a
	 * {@link WebMvcConfig#ORIGENS_CORS}: així l'endpoint no es pot fer servir per a redirigir a un
	 * lloc extern.
	 */
	@GetMapping("/sessioUsuari/login")
	public ResponseEntity<Void> login(
			@RequestParam(required = false) String retorn,
			HttpServletRequest request) {
		String desti = isRetornPermes(retorn, request) ?
				retorn :
				request.getContextPath() + BaseConfig.REACT_APP_PATH + "/";
		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(desti)).build();
	}

	private boolean isRetornPermes(String retorn, HttpServletRequest request) {
		if (retorn == null || retorn.isBlank()) {
			return false;
		}
		try {
			URI uri = new URI(retorn);
			if (!uri.isAbsolute()) {
				// Camí relatiu al mateix servidor; "//host" seria un altre servidor.
				return retorn.startsWith("/") && !retorn.startsWith("//");
			}
			String origen = uri.getScheme() + "://" + uri.getRawAuthority();
			String origenPropi = request.getScheme() + "://" + request.getServerName() +
					(request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":" + request.getServerPort());
			return origen.equalsIgnoreCase(origenPropi) || Arrays.asList(WebMvcConfig.ORIGENS_CORS).contains(origen);
		} catch (URISyntaxException ex) {
			return false;
		}
	}

	@Value
	public static class SessioUsuari {
		String codi;
		String nom;
		String email;
		List<String> rols;
	}

}
