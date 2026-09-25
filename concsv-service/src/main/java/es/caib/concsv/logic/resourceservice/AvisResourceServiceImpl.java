package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.model.AvisResource;
import es.caib.concsv.logic.intf.resourceservice.AvisResourceService;
import es.caib.concsv.persist.entity.AvisEntity;
import es.caib.concsv.persist.repository.AvisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementació del servei de gestió d'avisos.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvisResourceServiceImpl extends BaseMutableResourceService<AvisResource, Long, AvisEntity> implements AvisResourceService {

	private final AvisRepository avisRepository;

	@PostConstruct
	public void init() {
		ActivaActionExecutor activaActionExecutor = new ActivaActionExecutor();
		register(AvisResource.ACTION_ACTIVAR_CODE, activaActionExecutor);
		register(AvisResource.ACTION_DESACTIVAR_CODE, activaActionExecutor);
		register(AvisResource.ACTION_ACCIO_MASSIVA_CODE, new AccioMassivaActionExecutor());
	}

	@Override
	@Transactional(readOnly = true)
	public List<AvisResource> findActius(Long entitatId) {
		Date avui = DateUtils.truncate(new Date(), Calendar.DATE);
		return avisRepository.findActius(avui, entitatId).stream().
				map(this::entityToResource).
				collect(Collectors.toList());
	}

	/**
	 * Activa o desactiva l'avís segons el codi de l'acció executada. Les accions no tenen
	 * formulari (no declaren formClass), de manera que {@code params} sempre és null.
	 */
	private class ActivaActionExecutor implements ActionExecutor<AvisEntity, Serializable, Serializable> {

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
				AvisEntity entity,
				Serializable params) throws ActionExecutionException {
			entity.setActiu(AvisResource.ACTION_ACTIVAR_CODE.equals(code));
			avisRepository.save(entity);
			return null;
		}

	}

	/**
	 * Activa, desactiva o elimina múltiples avisos en una sola operació.
	 */
	private class AccioMassivaActionExecutor
			implements ActionExecutor<AvisEntity, AvisResource.FormAccioMassiva, Serializable> {

		@Override
		public void onChange(
				Serializable id,
				AvisResource.FormAccioMassiva previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				AvisResource.FormAccioMassiva target) {
			// El formulari no té cap camp que depengui dels altres.
		}

		@Override
		public Serializable exec(
				String code,
				AvisEntity entity,
				AvisResource.FormAccioMassiva params) throws ActionExecutionException {
			String accio = params.getAccio();
			if ("eliminar".equalsIgnoreCase(accio)) {
				avisRepository.deleteAllById(params.getIds());
			} else if ("activar".equalsIgnoreCase(accio) || "desactivar".equalsIgnoreCase(accio)) {
				List<AvisEntity> avisos = avisRepository.findAllById(params.getIds());
				avisos.forEach(avis -> avis.setActiu("activar".equalsIgnoreCase(accio)));
				avisRepository.saveAll(avisos);
			} else {
				throw new ActionExecutionException(
						AvisResource.class,
						null,
						code,
						"Tipus d'acció massiva desconegut: " + accio);
			}
			return null;
		}

	}

}
