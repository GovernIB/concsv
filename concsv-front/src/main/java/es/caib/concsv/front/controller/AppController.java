/**
 * 
 */
package es.caib.concsv.front.controller;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.caib.concsv.front.config.BaseConfig;

/**
 * Servei principal de l'API de l'aplicació.
 * 
 * @author Limit Tecnologies
 */
//@Hidden
@RestController
@RequestMapping(path = BaseConfig.API_PATH)
public class AppController {

	@GetMapping
	//@Operation(summary = "Consulta de l'índex de l'API")
	public ResponseEntity<CollectionModel<?>> index() {
		List<Link> indexLinks = new ArrayList<Link>();
		Link selfLink = linkTo(getClass()).withSelfRel();
		indexLinks.add(linkTo(methodOn(DocumentController.class).index()).withRel("document"));
		indexLinks.add(selfLink);
		CollectionModel<?> resources = CollectionModel.of(
				Collections.emptySet(),
				indexLinks.stream().toArray(Link[]::new));
		return ResponseEntity.ok(resources);
	}

}
