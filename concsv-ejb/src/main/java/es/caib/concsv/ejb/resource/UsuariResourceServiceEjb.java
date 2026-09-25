package es.caib.concsv.ejb.resource;

import es.caib.concsv.ejb.base.AbstractServiceEjb;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException.AnswerValue;
import es.caib.concsv.logic.intf.base.exception.ArtifactNotFoundException;
import es.caib.concsv.logic.intf.base.exception.ReportGenerationException;
import es.caib.concsv.logic.intf.base.exception.ResourceFieldNotFoundException;
import es.caib.concsv.logic.intf.base.model.ResourceArtifactType;
import es.caib.concsv.logic.intf.resourceservice.UsuariResourceService;
import lombok.experimental.Delegate;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * EJB que exposa el servei Spring {@link UsuariResourceService} al WAR del backoffice (mode JBoss).
 *
 * @author Límit Tecnologies
 */
@Local(UsuariResourceService.class)
@Stateless
@RolesAllowed("**")
public class UsuariResourceServiceEjb extends AbstractServiceEjb<UsuariResourceService> implements UsuariResourceService {

	@Delegate
	private UsuariResourceService delegateService;

	@Override
	protected void setDelegateService(UsuariResourceService delegateService) {
		this.delegateService = delegateService;
	}

	// Lombok no genera la delegació dels mètodes genèrics: s'escriuen a mà.

	@Override
	public <P extends Serializable> Serializable artifactActionExec(String id, String code, P params)
			throws ArtifactNotFoundException, ActionExecutionException {
		return delegateService.artifactActionExec(id, code, params);
	}

	@Override
	public <P extends Serializable> Map<String, Object> artifactOnChange(ResourceArtifactType type, String code,
			String id, P previous, String fieldName, Object fieldValue, Map<String, AnswerValue> answers)
			throws ArtifactNotFoundException, ResourceFieldNotFoundException, AnswerRequiredException {
		return delegateService.artifactOnChange(type, code, id, previous, fieldName, fieldValue, answers);
	}

	@Override
	public <P extends Serializable> List<?> artifactReportGenerateData(String id, String code, P params)
			throws ArtifactNotFoundException, ReportGenerationException {
		return delegateService.artifactReportGenerateData(id, code, params);
	}

}
