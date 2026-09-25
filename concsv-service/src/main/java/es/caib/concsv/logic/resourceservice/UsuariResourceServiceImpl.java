package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.helper.AuthenticationHelper;
import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.intf.base.model.FieldOption;
import es.caib.concsv.logic.intf.base.util.I18nUtil;
import es.caib.concsv.logic.intf.model.IdiomaEnum;
import es.caib.concsv.logic.intf.model.UsuariResource;
import es.caib.concsv.logic.intf.model.auth.AuthenticationDetails;
import es.caib.concsv.logic.intf.resourceservice.UsuariResourceService;
import es.caib.concsv.persist.entity.UsuariEntity;
import es.caib.concsv.persist.repository.UsuariRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.ClaimAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Implementació del servei de consulta i modificació del perfil de l'usuari autenticat actual.
 * <p>
 * Restringeix sempre l'accés al propi usuari: independentment de l'id sol·licitat, la consulta
 * només pot retornar (o modificar) el registre de l'usuari autenticat.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuariResourceServiceImpl extends BaseMutableResourceService<UsuariResource, String, UsuariEntity> implements UsuariResourceService {

	private static final String ROLE_DISPLAY_PREFIX = "CSV_";

	private final AuthenticationHelper authenticationHelper;
	private final UsuariRepository usuariRepository;

	@PostConstruct
	public void init() {
		register(UsuariResource.Fields.idioma, new IdiomaFieldOptionsProvider());
	}

	@Override
	protected Specification<UsuariEntity> additionalSpecification(String[] namedQueries) {
		String currentUserName = authenticationHelper.getCurrentUserName();
		return (root, query, cb) -> cb.equal(root.get("id"), currentUserName);
	}

	/**
	 * Es crida sempre en convertir l'entitat (consultes i resposta de create/update), així que és
	 * el lloc on omplir els camps derivats que no es persisteixen.
	 */
	@Override
	protected void afterConversion(UsuariEntity entity, UsuariResource resource) {
		String[] roles = authenticationHelper.getCurrentUserRoles();
		resource.setRols(Arrays.stream(roles).
				filter(r -> r.startsWith(ROLE_DISPLAY_PREFIX)).
				toArray(String[]::new));
	}

	@Override
	@Transactional
	public void refresh() {
		DadesUsuari dades = getDadesUsuariAutenticat();
		if (dades == null) {
			return;
		}
		UsuariEntity usuari = usuariRepository.findById(dades.codi).orElse(null);
		if (usuari == null) {
			log.info("Alta de l'usuari {} al backoffice", dades.codi);
			usuari = new UsuariEntity();
			usuari.setId(dades.codi);
			actualitzarDades(usuari, dades);
			usuariRepository.save(usuari);
		} else if (!Objects.equals(usuari.getNom(), dades.nom) ||
				!Objects.equals(usuari.getNif(), dades.nif) ||
				!Objects.equals(usuari.getEmail(), dades.email)) {
			actualitzarDades(usuari, dades);
			usuariRepository.save(usuari);
		}
	}

	private void actualitzarDades(UsuariEntity usuari, DadesUsuari dades) {
		usuari.setNom(dades.nom);
		usuari.setNif(dades.nif);
		usuari.setEmail(dades.email);
	}

	/**
	 * Obté les dades personals de l'usuari autenticat de l'objecte d'autenticació. N'hi ha de tres
	 * tipus segons com s'ha autenticat:
	 * <ul>
	 * <li>JBoss (adaptador Keycloak): els detalls implementen {@link AuthenticationDetails}.</li>
	 * <li>Spring Boot amb login OIDC (sessió) o amb token Bearer: el principal és un
	 * {@code OidcUser} o un {@code Jwt}, i tots dos donen accés als claims del token.</li>
	 * </ul>
	 */
	private DadesUsuari getDadesUsuariAutenticat() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
			return null;
		}
		DadesUsuari dades = new DadesUsuari();
		dades.codi = auth.getName();
		if (auth.getDetails() instanceof AuthenticationDetails) {
			AuthenticationDetails details = (AuthenticationDetails)auth.getDetails();
			dades.nom = details.getName();
			dades.nif = details.getNif();
			dades.email = details.getEmail();
		} else if (auth.getPrincipal() instanceof ClaimAccessor) {
			ClaimAccessor claims = (ClaimAccessor)auth.getPrincipal();
			dades.nom = claims.getClaimAsString("name");
			dades.nif = claims.getClaimAsString("nif");
			dades.email = claims.getClaimAsString("email");
		}
		if (dades.nif != null && dades.nif.length() > 9) {
			// La columna és de 9 caràcters: un valor més llarg no és un NIF vàlid.
			dades.nif = null;
		}
		return dades;
	}

	private static class DadesUsuari {
		private String codi;
		private String nom;
		private String nif;
		private String email;
	}

	/**
	 * Valors del desplegable d'idioma. El valor de l'opció és el nom de la constant, que és el que
	 * es desa a la columna.
	 */
	private static class IdiomaFieldOptionsProvider implements FieldOptionsProvider {
		@Override
		public List<FieldOption> getOptions(String fieldName, Map<String, String[]> requestParameterMap) {
			return Arrays.stream(IdiomaEnum.values()).
					map(idioma -> new FieldOption(
							idioma.name(),
							I18nUtil.getInstance().getI18nMessage(
									IdiomaEnum.class.getName() + "." + idioma.name()))).
					collect(Collectors.toList());
		}
	}

}
