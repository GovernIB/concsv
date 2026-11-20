package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.helper.AuthenticationHelper;
import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.helper.ExecucioMassivaCuaHelper;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaResource;
import es.caib.concsv.logic.intf.resourceservice.ExecucioMassivaResourceService;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.entity.UsuariEntity;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.UsuariRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;

/**
 * Implementació del servei de consulta i cancel·lació de les execucions massives.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecucioMassivaResourceServiceImpl
		extends BaseMutableResourceService<ExecucioMassivaResource, Long, ExecucioMassivaEntity>
		implements ExecucioMassivaResourceService {

	private final ExecucioMassivaContingutRepository contingutRepository;
	private final ExecucioMassivaCuaHelper cuaHelper;
	private final AuthenticationHelper authenticationHelper;
	private final UsuariRepository usuariRepository;

	@PostConstruct
	public void init() {
		register(ExecucioMassivaResource.ACTION_CANCELAR_CODE, new CancelarActionExecutor());
	}

	/**
	 * Calcula els recomptes d'elements per estat (una sola consulta per execució, sense carregar els
	 * elements) i si es pot cancel·lar.
	 */
	@Override
	protected void afterConversion(ExecucioMassivaEntity entity, ExecucioMassivaResource resource) {
		Map<ExecucioMassivaContingutEstatEnum, Long> recomptes = new EnumMap<>(ExecucioMassivaContingutEstatEnum.class);
		for (Object[] fila : contingutRepository.countPerEstat(entity.getId())) {
			recomptes.put((ExecucioMassivaContingutEstatEnum) fila[0], ((Number) fila[1]).longValue());
		}
		resource.setFinalitzats(recomptes.getOrDefault(ExecucioMassivaContingutEstatEnum.FINALITZAT, 0L).intValue());
		resource.setErrors(recomptes.getOrDefault(ExecucioMassivaContingutEstatEnum.ERROR, 0L).intValue());
		resource.setPendents(recomptes.getOrDefault(ExecucioMassivaContingutEstatEnum.PENDENT, 0L).intValue());
		resource.setCancelats(recomptes.getOrDefault(ExecucioMassivaContingutEstatEnum.CANCELAT, 0L).intValue());
		resource.setTotal(recomptes.values().stream().mapToInt(Long::intValue).sum());
		resource.setCancelable(esCancelable(entity));
		resource.setCreatedByFullName(nomComplet(entity.getCreatedBy()));
	}

	/**
	 * L'usuari tal com es mostra, "Nom (codi)", llegit del seu perfil: a la BD només es desa el codi. Si l'usuari
	 * no té perfil o no en té el nom, es mostra només el codi.
	 */
	String nomComplet(String codi) {
		if (codi == null) {
			return null;
		}
		return usuariRepository.findById(codi).
				map(UsuariEntity::getNom).
				filter(nom -> !nom.trim().isEmpty()).
				map(nom -> nom.trim() + " (" + codi + ")").
				orElse(codi);
	}

	/** Es pot cancel·lar si el tipus ho permet, no ha acabat i no s'ha demanat ja la cancel·lació. */
	static boolean esCancelable(ExecucioMassivaEntity entity) {
		return entity.getTipus() != null
				&& entity.getTipus().isCancelable()
				&& entity.getDataFi() == null
				&& entity.getCancelacioData() == null;
	}

	/**
	 * Cancel·la l'execució. El resultat és {@link ExecucioMassivaResource#RESULTAT_CANCELADA} si s'ha tancat
	 * a l'instant, o {@link ExecucioMassivaResource#RESULTAT_EN_CURS} si només s'ha demanat l'aturada.
	 */
	private class CancelarActionExecutor implements ActionExecutor<ExecucioMassivaEntity, Serializable, ExecucioMassivaResource.ResultatCancelacio> {

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
		public ExecucioMassivaResource.ResultatCancelacio exec(
				String code,
				ExecucioMassivaEntity entity,
				Serializable params) throws ActionExecutionException {
			try {
				return new ExecucioMassivaResource.ResultatCancelacio(
						cuaHelper.cancelar(entity.getId(), authenticationHelper.getCurrentUserName()));
			} catch (IllegalStateException ex) {
				throw new ActionExecutionException(ExecucioMassivaResource.class, null, code, ex.getMessage());
			}
		}

	}

}
