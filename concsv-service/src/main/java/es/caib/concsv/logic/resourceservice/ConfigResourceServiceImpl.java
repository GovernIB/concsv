package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.helper.ConfigHelper;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.base.exception.ResourceNotUpdatedException;
import es.caib.concsv.logic.intf.model.ConfigResource;
import es.caib.concsv.logic.intf.resourceservice.ConfigResourceService;
import es.caib.concsv.persist.entity.ConfigEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Implementació del servei de gestió de propietats configurables.
 * <p>
 * Només es pot modificar el valor de les propietats que no són del fitxer de propietats del
 * servidor. En desar-lo es valida segons el tipus de la propietat; els serveis llegeixen el valor
 * de la base de dades a cada ús (veure {@link ConfigHelper}).
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigResourceServiceImpl extends BaseMutableResourceService<ConfigResource, String, ConfigEntity> implements ConfigResourceService {

	/** Codi del tipus de les propietats que contenen credencials. */
	static final String TIPUS_CREDENCIALS = "CREDENTIALS";
	static final String TIPUS_BOOL = "BOOL";
	static final String TIPUS_INT = "INT";
	/** Valor amb què es substitueixen les credencials: mai surten per l'API. */
	static final String VALOR_EMMASCARAT = "********";

	/** Valors que accepta una propietat booleana (sense distingir majúscules): true/false i S/N. */
	private static final Set<String> VALORS_BOOLEANS = new HashSet<>(Arrays.asList("true", "false", "s", "n"));
	private static final Pattern ENTER = Pattern.compile("-?\\d{1,18}");

	private final ConfigHelper configHelper;

	@PostConstruct
	public void init() {
		register(ConfigResource.ACTION_SYNC_JBOSS_CODE, new SincronitzaActionExecutor());
	}

	/**
	 * Les propietats del fitxer de propietats del servidor no es poden modificar des del
	 * backoffice. Les credencials tampoc es poden desar a la base de dades (quedarien en clar):
	 * només es defineixen als fitxers del servidor. La resta es valida segons el seu tipus.
	 */
	@Override
	protected void beforeUpdateEntity(
			ConfigEntity entity,
			ConfigResource resource,
			Map<String, AnswerRequiredException.AnswerValue> answers) throws ResourceNotUpdatedException {
		if (entity.isJbossProperty()) {
			throw noModificada(entity, "La propietat es defineix al fitxer de propietats del servidor");
		}
		// Manen la clau i el tipus de la fila, no els que arribin al cos de la petició.
		resource.setKey(entity.getKey());
		String valor = resource.getValue();
		if (valor != null && valor.trim().isEmpty()) {
			// Un valor buit equival a no tenir valor propi
			valor = null;
		}
		if (esCredencial(entity)) {
			if (valor != null) {
				if (!VALOR_EMMASCARAT.equals(valor)) {
					throw noModificada(entity, "Les credencials es defineixen als fitxers de propietats del servidor");
				}
				// El formulari retorna el valor emmascarat si l'usuari no l'ha canviat.
				valor = entity.getValue();
			}
		} else {
			valor = validaValor(entity, valor);
		}
		resource.setValue(valor);
	}

	/** Només es registra qui i quina propietat: els valors poden ser sensibles. */
	@Override
	protected void beforeUpdateSave(
			ConfigEntity entity,
			ConfigResource resource,
			Map<String, AnswerRequiredException.AnswerValue> answers) {
		log.info("Propietat {} modificada per {}", entity.getKey(), usuariActual());
	}

	/**
	 * Les propietats del fitxer del servidor mostren el valor del fitxer. A les editables sense valor
	 * a la base de dades, {@code alternativeValue} duu el valor del fitxer, que és el que s'aplica mentre no en tinguin.
	 */
	@Override
	protected void afterConversion(ConfigEntity entity, ConfigResource resource) {
		String fitxer = configHelper.getEnvironmentProperty(entity.getKey(), null);
		if (entity.isJbossProperty()) {
			resource.setValue(fitxer);
		} else if (entity.getValue() == null) {
			resource.setAlternativeValue(fitxer);
		}
		if (esCredencial(entity)) {
			if (resource.getValue() != null) {
				resource.setValue(VALOR_EMMASCARAT);
			}
			if (resource.getAlternativeValue() != null) {
				resource.setAlternativeValue(VALOR_EMMASCARAT);
			}
		}
	}

	/**
	 * Valida el valor segons el tipus de la propietat: enter per a INT, true/false/S/N per a BOOL i,
	 * si el tipus és una enumeració, un dels seus valors. Un valor nul sempre és vàlid.
	 *
	 * @return el valor sense espais als extrems (INT i BOOL).
	 */
	private String validaValor(ConfigEntity entity, String valor) {
		if (valor == null) {
			return null;
		}
		String tipus = entity.getType() != null ? entity.getType().getCode() : null;
		if (TIPUS_INT.equals(tipus)) {
			if (!ENTER.matcher(valor.trim()).matches()) {
				throw noModificada(entity, "El valor ha de ser un nombre enter");
			}
			return valor.trim();
		}
		if (TIPUS_BOOL.equals(tipus)) {
			if (!VALORS_BOOLEANS.contains(valor.trim().toLowerCase())) {
				throw noModificada(entity, "El valor ha de ser true, false, S o N");
			}
			return valor.trim();
		}
		if (entity.getType() != null) {
			List<String> valids = entity.getType().getValidValues();
			if (!valids.isEmpty() && !valids.contains(valor)) {
				throw noModificada(entity, "El valor ha de ser un de: " + String.join(", ", valids));
			}
		}
		return valor;
	}

	private ResourceNotUpdatedException noModificada(ConfigEntity entity, String motiu) {
		return new ResourceNotUpdatedException(getResourceClass(), entity.getId(), motiu);
	}

	private boolean esCredencial(ConfigEntity entity) {
		return entity.getType() != null && TIPUS_CREDENCIALS.equals(entity.getType().getCode());
	}

	private String usuariActual() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null ? authentication.getName() : "desconegut";
	}

	/**
	 * Copia a la base de dades el valor dels fitxers del servidor (veure
	 * {@link ConfigHelper#synchronize()}). Retorna el nombre de propietats actualitzades. L'acció no
	 * té formulari ni identificador.
	 */
	private class SincronitzaActionExecutor implements ActionExecutor<ConfigEntity, Serializable, Serializable> {

		@Override
		public void onChange(
				Serializable id,
				Serializable previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				Serializable target) {
			// Sense formulari no hi ha cap camp que pugui canviar.
		}

		@Override
		public Serializable exec(
				String code,
				ConfigEntity entity,
				Serializable params) throws ActionExecutionException {
			return configHelper.synchronize();
		}

	}

}
