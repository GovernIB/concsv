package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.helper.AuthenticationHelper;
import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.base.model.FileReference;
import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.EntitatResource;
import es.caib.concsv.logic.intf.resourceservice.EntitatResourceService;
import es.caib.concsv.persist.entity.EntitatEntity;
import es.caib.concsv.persist.repository.AvisRepository;
import es.caib.concsv.persist.repository.EntitatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Map;

/**
 * Implementació del servei de gestió d'entitats.
 *
 * @author Límit Tecnologies
 */
@Service
@RequiredArgsConstructor
public class EntitatResourceServiceImpl extends BaseMutableResourceService<EntitatResource, Long, EntitatEntity> implements EntitatResourceService {

	private final AuthenticationHelper authenticationHelper;
	private final EntitatRepository entitatRepository;
	private final AvisRepository avisRepository;

	@PostConstruct
	public void init() {
		// Un sol executor per als dos codis: la lògica és la mateixa i el codi de l'acció arriba
		// com a paràmetre d'exec().
		ActivaActionExecutor activaActionExecutor = new ActivaActionExecutor();
		register(EntitatResource.ACTION_ACTIVAR_CODE, activaActionExecutor);
		register(EntitatResource.ACTION_DESACTIVAR_CODE, activaActionExecutor);
		register(EntitatResource.Fields.logoImgFile, new EntitatImagesOnchangeLogicProcessor());
		register(EntitatResource.Fields.logoImgFileDark, new EntitatImagesOnchangeLogicProcessor());
	}

	/**
	 * El superusuari administra totes les entitats; la resta d'usuaris només veuen les actives, que
	 * són les que es poden triar al selector d'entitat de la capçalera.
	 */
	@Override
	protected Specification<EntitatEntity> additionalSpecification(String[] namedQueries) {
		if (authenticationHelper.isCurrentUserInRole(BaseConfig.ROLE_SUPER)) {
			return null;
		}
		return (root, query, cb) -> cb.isTrue(root.get(EntitatResource.Fields.activa));
	}

	/**
	 * Els avisos de l'entitat no tenen sentit sense ella i impedirien esborrar-la (clau forana).
	 */
	@Override
	protected void beforeDelete(
			EntitatEntity entity,
			Map<String, AnswerRequiredException.AnswerValue> answers) {
		avisRepository.deleteByEntitatId(entity.getId());
	}

	/**
	 * Activa o desactiva l'entitat segons el codi de l'acció executada. Les accions no tenen
	 * formulari (no declaren formClass), de manera que {@code params} sempre és null.
	 */
	private class ActivaActionExecutor implements ActionExecutor<EntitatEntity, Serializable, Serializable> {

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
				EntitatEntity entity,
				Serializable params) throws ActionExecutionException {
			entity.setActiva(EntitatResource.ACTION_ACTIVAR_CODE.equals(code));
			entitatRepository.save(entity);
			return null;
		}

	}

	/**
	 * Copia el contingut dels logos pujats des del formulari als camps que es persisteixen.
	 */
	private static class EntitatImagesOnchangeLogicProcessor implements OnChangeLogicProcessor<EntitatResource> {

		@Override
		public void onChange(
				Serializable id,
				EntitatResource previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				EntitatResource target) {
			byte[] contingut = fieldValue != null ? ((FileReference)fieldValue).getContent() : null;
			if (EntitatResource.Fields.logoImgFile.equals(fieldName)) {
				target.setLogoImgBytes(contingut);
			} else if (EntitatResource.Fields.logoImgFileDark.equals(fieldName)) {
				target.setLogoImgBytesDark(contingut);
			}
		}

	}

}
