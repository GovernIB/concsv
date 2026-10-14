package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseMutableResourceService;
import es.caib.concsv.logic.helper.IntegracioDiagnosticHelper;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.base.util.I18nUtil;
import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource;
import es.caib.concsv.logic.intf.resourceservice.MonitorIntegracioResourceService;
import es.caib.concsv.persist.entity.MonitorIntegracioEntity;
import es.caib.concsv.persist.repository.MonitorIntegracioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementació del servei de consulta del monitor d'integracions.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorIntegracioResourceServiceImpl
		extends BaseMutableResourceService<MonitorIntegracioResource, Long, MonitorIntegracioEntity>
		implements MonitorIntegracioResourceService {

	private final IntegracioDiagnosticHelper integracioDiagnosticHelper;
	private final MonitorIntegracioRepository monitorIntegracioRepository;

	@PostConstruct
	public void init() {
		register(MonitorIntegracioResource.ACTION_PIPELLES_CODE, new PipellesActionExecutor());
		register(MonitorIntegracioResource.ACTION_INTEGRACIONS_DIAGNOSTIC_CODE, new IntegracionsDiagnosticActionExecutor());
		register(MonitorIntegracioResource.ACTION_DIAGNOSTIC_CODE, new DiagnosticActionExecutor());
	}

	/**
	 * El mapeig automàtic no copia els paràmetres (són un mapa desat com a JSON), així que es lliuren aquí.
	 */
	@Override
	protected void afterConversion(MonitorIntegracioEntity entity, MonitorIntegracioResource resource) {
		resource.setParametres(entity.getParametres());
	}

	/**
	 * Cada pestanya del llistat és una "named query" amb el codi de la integració: 
	 * filtra per aquest codi sense que el client l'hagi d'enviar dins el filtre. 
	 * Un codi que no existeix no retorna cap fila (mai totes).
	 */
	@Override
	protected <P> Specification<P> namedQueryToSpecification(String namedQuery) {
		return (root, query, cb) -> {
			try {
				return cb.equal(root.get("codi"), IntegracioCodiEnum.valueOf(namedQuery));
			} catch (IllegalArgumentException | NullPointerException ex) {
				return cb.disjunction();
			}
		};
	}

	/**
	 * Pestanyes del llistat: una per integració, en l'ordre de {@link IntegracioCodiEnum}, amb el
	 * nombre de peticions amb error que té cadascuna.
	 */
	List<MonitorIntegracioResource.Pipella> pipelles() {
		Map<IntegracioCodiEnum, Long> errors = recompteErrors();
		return Arrays.stream(IntegracioCodiEnum.values()).
				map(codi -> new MonitorIntegracioResource.Pipella(
						codi.name(),
						nomIntegracio(codi),
						errors != null ? errors.getOrDefault(codi, 0L) : null)).
				collect(Collectors.toList());
	}

	/**
	 * Nombre de peticions amb error per integració. Si el recompte falla (p. ex. la base de dades no
	 * respon) retorna {@code null}: les pestanyes s'han de poder mostrar igualment, només sense badge.
	 */
	private Map<IntegracioCodiEnum, Long> recompteErrors() {
		try {
			Map<IntegracioCodiEnum, Long> errors = new EnumMap<>(IntegracioCodiEnum.class);
			for (Object[] fila : monitorIntegracioRepository.countPerCodiByEstat(IntegracioAccioEstatEnum.ERROR)) {
				errors.put((IntegracioCodiEnum) fila[0], ((Number) fila[1]).longValue());
			}
			return errors;
		} catch (RuntimeException ex) {
			log.warn("No s'ha pogut comptar les peticions amb error del monitor d'integracions", ex);
			return null;
		}
	}

	/** Nom traduït de la integració; si no té traducció, el seu codi. */
	String nomIntegracio(IntegracioCodiEnum codi) {
		try {
			return I18nUtil.getInstance().getI18nMessage(IntegracioCodiEnum.class.getName() + "." + codi.name());
		} catch (RuntimeException ex) {
			return codi.name();
		}
	}

	/** Pestanyes del llistat amb el nombre d'errors de cada integració. */
	private class PipellesActionExecutor implements
			ActionExecutor<MonitorIntegracioEntity, Serializable, MonitorIntegracioResource.PipellesResult> {

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
		public MonitorIntegracioResource.PipellesResult exec(
				String code,
				MonitorIntegracioEntity entity,
				Serializable params) throws ActionExecutionException {
			return new MonitorIntegracioResource.PipellesResult(pipelles());
		}

	}

	/** Codis de les integracions que es poden diagnosticar. */
	private class IntegracionsDiagnosticActionExecutor implements
			ActionExecutor<MonitorIntegracioEntity, Serializable, MonitorIntegracioResource.IntegracionsDiagnosticResult> {

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
		public MonitorIntegracioResource.IntegracionsDiagnosticResult exec(
				String code,
				MonitorIntegracioEntity entity,
				Serializable params) throws ActionExecutionException {
			return new MonitorIntegracioResource.IntegracionsDiagnosticResult(integracioDiagnosticHelper.codis());
		}

	}

	/**
	 * Prova una integració. Un error de la integració no és un error de l'acció: es retorna com a
	 * resultat amb {@code correcte = false}.
	 */
	private class DiagnosticActionExecutor implements
			ActionExecutor<MonitorIntegracioEntity, MonitorIntegracioResource.DiagnosticForm, MonitorIntegracioResource.DiagnosticResult> {

		@Override
		public void onChange(
				Serializable id,
				MonitorIntegracioResource.DiagnosticForm previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				MonitorIntegracioResource.DiagnosticForm target) {
			// El formulari és un sol camp: no n'hi ha cap altre que en depengui.
		}

		@Override
		public MonitorIntegracioResource.DiagnosticResult exec(
				String code,
				MonitorIntegracioEntity entity,
				MonitorIntegracioResource.DiagnosticForm params) throws ActionExecutionException {
			String codiIntegracio = params != null ? params.getCodiIntegracio() : null;
			IntegracioCodiEnum codi;
			try {
				codi = IntegracioCodiEnum.valueOf(codiIntegracio);
			} catch (IllegalArgumentException | NullPointerException ex) {
				throw new ActionExecutionException(
						MonitorIntegracioResource.class,
						null,
						code,
						"Codi d'integració no diagnosticable: " + codiIntegracio);
			}
			return integracioDiagnosticHelper.executa(codi);
		}

	}

}
