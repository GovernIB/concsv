/**
 *
 */
package es.caib.concsv.front.controller;

import es.caib.concsv.front.config.BaseConfig;
import es.caib.concsv.front.interficies.ThrowingSupplier;
import es.caib.concsv.logic.intf.enums.ResultTypeEnum;
import es.caib.concsv.logic.intf.exception.DocumentNotExistException;
import es.caib.concsv.logic.intf.exception.DuplicatedHashException;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import es.caib.concsv.logic.intf.model.*;
import es.caib.concsv.logic.intf.resourceservice.AvisResourceService;
import es.caib.concsv.logic.intf.service.ComandaServiceInterface;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Servei de consulta de avisos.
 *
 * @author Limit Tecnologies
 */
@Slf4j
@RestController
@RequestMapping(path = BaseConfig.API_PATH + "/avisos")
public class AvisController {

	@Autowired(required = false)
	private AvisResourceService avisResourceService;
	@Autowired(required = false)
	private ComandaServiceInterface comandaService;

	/** Endpoint raíz para descubrir los enlaces disponibles (HATEOAS) */
	@GetMapping
	public ResponseEntity<CollectionModel<?>> index() {
		log.debug("Consultant index del servei d'avisos");
		List<Link> links = new ArrayList<>();
		try {
			links.add(linkTo(methodOn(getClass()).getActius(null, "ca")).withRel("actius"));
		} catch (Exception e) {
			log.warn("Error generant links HATEOAS per a avisos", e);
		}
		links.add(linkTo(getClass()).withSelfRel());
		return ResponseEntity.ok(
			CollectionModel.of(Collections.emptySet(), links.toArray(Link[]::new))
		);
	}

	/** Retorna la llista d'avisos actius, filtrant per entitat de forma opcional. **/
	@GetMapping(value = "/actius")
	public ResponseEntity<CollectionModel<AvisResource>> getActius(
		@RequestParam(name = "entitatId", required = false) final Long entitatId,
		@RequestParam(name = "lang", required = false, defaultValue = "ca") final String lang)
		throws DuplicatedHashException, DocumentNotExistException, GenericServiceException, IOException {
		return ejecutarYRegistrar("Avisos d'entitats", () -> {
			log.debug("Consultar avisos entitat (entitat={}, lang={})", entitatId, lang);
			List<AvisResource> result = avisResourceService.findActius(entitatId);
			List<Link> links = new ArrayList<>();
			links.add(linkTo(methodOn(getClass()).getActius(entitatId, lang)).withSelfRel());
			return ResponseEntity.ok(CollectionModel.of(result, links));
		});
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
				comandaService.registrarPeticio(fullKey, resultType, System.currentTimeMillis() - start);
			} catch (Exception e) {
				log.warn("No s'ha pogut registrar la estadística per {}", fullKey, e);
			}
		}
	}

}
