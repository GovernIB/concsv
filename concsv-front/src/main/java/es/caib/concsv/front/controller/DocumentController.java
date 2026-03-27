/**
 * 
 */
package es.caib.concsv.front.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import es.caib.concsv.front.interficies.ThrowingSupplier;
import es.caib.concsv.persistence.model.EnviamentOrigen;
import es.caib.concsv.persistence.model.EnviamentTipus;
import es.caib.concsv.service.enums.ResultTypeEnum;
import es.caib.concsv.service.facade.EstadisticaServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.RequestContextUtils;

import es.caib.concsv.front.config.BaseConfig;
import es.caib.concsv.service.exception.DocumentNotExistException;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.facade.HashServiceInterface;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * Servei de consulta de documents.
 * 
 * @author Limit Tecnologies
 */
@Slf4j
@RestController
@RequestMapping(path = BaseConfig.API_PATH + "/documents")
public class DocumentController {

	@Autowired(required = false)
	private HashServiceInterface hashService;
    @Autowired(required = false)
    private EstadisticaServiceInterface estadisticaService;

	@GetMapping
	//@Operation(summary = "Consulta de l'índex del servei")
	public ResponseEntity<CollectionModel<?>> index() {
		log.debug("Consultant index del servei de documents");
		List<Link> links = new ArrayList<>();
		try {
			links.add(linkTo(methodOn(getClass()).getOne(null)).withRel("getOne"));
			links.add(linkTo(methodOn(getClass()).downloadOriginal(null, false)).withRel("downloadOriginal"));
			links.add(linkTo(methodOn(getClass()).downloadPrintable(null, false, null)).withRel("downloadPrintable"));
		} catch (Exception ignored) { }
		links.add(linkTo(getClass()).withSelfRel());
		return ResponseEntity.ok(
				CollectionModel.of(
						Collections.emptySet(),
						links.toArray(Link[]::new)));
	}

	/** Consulta la informació del document i retorna la resposta per a que es mostri la informació a la pàgina.
	 * 
	 * @param csv
	 * @return
	 * @throws DocumentNotExistException
	 * @throws GenericServiceException
	 * @throws DuplicatedHashException
	 */
	@GetMapping(value = "/{csv}")
	//@Operation(summary = "Consulta la informació d'un document")
	public ResponseEntity<EntityModel<DocumentInfo>> getOne(
			@PathVariable
			//@Parameter(description = "Codi CSV del document")
			final String csv) throws DocumentNotExistException, GenericServiceException, DuplicatedHashException, IOException {
        return ejecutarYRegistrar(EnviamentTipus.METADATOS.name(), () -> {
            log.debug("Consultant document (csv=" + csv + ")");
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo != null) {
                List<Link> links = new ArrayList<>();
                links.add(linkTo(methodOn(getClass()).getOne(csv)).withSelfRel());
                return ResponseEntity.ok(EntityModel.of(documentInfo, links));
            } else {
                throw new DocumentNotExistException();
            }
        });
	}

	@GetMapping("/{csv}/original")
	//@Operation(summary = "Descarrega el document original")
	public ResponseEntity<InputStreamResource> downloadOriginal(
			@PathVariable
			//@Parameter(description = "Codi CSV del document")
			final String csv,
			@RequestParam(name = "preview", required = false)
			final boolean preview) throws GenericServiceException, DuplicatedHashException, DocumentNotExistException, IOException {
        return ejecutarYRegistrar(EnviamentTipus.ORIGINAL.name(), () -> {
            log.debug("Descarregant document original (csv=" + csv + ")");
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo != null) {
                DocumentContent content = hashService.getDocument(documentInfo, true);
                return writeDocumentContentToResponse(content, !preview);
            } else {
                throw new DocumentNotExistException();
            }
        });
	}

	@GetMapping("/{csv}/printable")
	//@Operation(summary = "Descarrega la versió imprimible del document")
	public ResponseEntity<InputStreamResource> downloadPrintable(
			@PathVariable
			//@Parameter(description = "Codi CSV del document")
			final String csv,
			@RequestParam(name = "preview", required = false)
			final boolean preview,
			@RequestParam(name = "lang", required = false, defaultValue = "ca")
			final String lang) throws GenericServiceException, DuplicatedHashException, DocumentNotExistException, IOException {
        return ejecutarYRegistrar(EnviamentTipus.IMPRIMIBLE.name(), () -> {
            log.debug("Descarregant versió imprimible (csv=" + csv + ")");
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo != null) {
                DocumentContent content = hashService.getPrintableDocument(documentInfo, lang);
                return writeDocumentContentToResponse(content, !preview);
            } else {
                throw new DocumentNotExistException();
            }
        });
	}

	@GetMapping("/{csv}/preview")
	//@Operation(summary = "Descarrega la versió imprimible del document")
	public ResponseEntity<InputStreamResource> downloadPreview(
			@PathVariable
			//@Parameter(description = "Codi CSV del document")
			final String csv,
			@RequestParam(name = "lang", required = false, defaultValue = "ca")
			final String lang) throws GenericServiceException, DuplicatedHashException, DocumentNotExistException, IOException {
        return ejecutarYRegistrar(EnviamentTipus.IMPRIMIBLE.name(), () -> { //TODO entrada nova
            log.debug("Descarregant la previsualització (csv=" + csv + ")");
            DocumentInfo documentInfo = hashService.checkHash(csv);
            if (documentInfo == null) {
                throw new DocumentNotExistException();
            }
            DocumentContent content = null;
            if (!Boolean.TRUE.equals(documentInfo.getMalformatted())) {
                try {
                    content = hashService.getPrintableDocument(documentInfo, lang);
                } catch (Exception ignored) {}
            }
            if (content == null) {
                content = hashService.getDocument(documentInfo, true);
            }
            return writeDocumentContentToResponse(content, false);
        });
	}

	private ResponseEntity<InputStreamResource> writeDocumentContentToResponse(
			DocumentContent content,
			boolean includeDownloadHeaders) throws IOException {
		BodyBuilder bodyBuilder = ResponseEntity.ok();
		InputStreamResource resource;
		if (content.getContent() != null) {
			resource = new InputStreamResource(new ByteArrayInputStream(content.getContent()));
			if (includeDownloadHeaders) {
				HttpHeaders headers = new HttpHeaders();
				headers.set("Content-Disposition", "attachment; filename=" + content.getFileName());
				headers.set("Access-Control-Expose-Headers", "Content-Disposition");
				bodyBuilder.headers(headers);
			}
			MediaType mediaType = content.getMimeType() != null ? MediaType.valueOf(content.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM;
			bodyBuilder.contentType(mediaType);
			bodyBuilder.contentLength(content.getContent().length);
		} else {
			resource = new InputStreamResource(new ByteArrayInputStream(new byte[0]));
		}
		return bodyBuilder.body(resource);
	}

	private String getCurrentLanguage() {
		HttpServletRequest request = getCurrentHttpServletRequest();
		Locale currentLocale = RequestContextUtils.getLocaleResolver(request).resolveLocale(request);
		return currentLocale.getLanguage();
	}

	private HttpServletRequest getCurrentHttpServletRequest() {
		return ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getRequest();
	}

    private <T, E extends Exception> ResponseEntity<T> ejecutarYRegistrar(
            /*@NotNull*/ String key,
            ThrowingSupplier<ResponseEntity<T>> accion) throws DocumentNotExistException, GenericServiceException, DuplicatedHashException, IOException {

        long start = System.currentTimeMillis();
        String fullKey = key + ":" + EnviamentOrigen.FRONT.name();
        ResultTypeEnum resultType = ResultTypeEnum.ERROR;

        try {
            ResponseEntity<T> response = accion.get();

            int status = response.getStatusCodeValue();
            if (status == 204) {
                resultType = ResultTypeEnum.INVALID;
            } else if (status >= 200 && status < 300) {
                resultType = ResultTypeEnum.OK;
            }

            return response;
        } finally {
            try {
                estadisticaService.registrarPeticio(fullKey, resultType, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("No s'ha pogut registrar la estadística per {}: {}", fullKey, e.getMessage());
            }
        }
    }

}
