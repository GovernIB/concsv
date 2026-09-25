package es.caib.concsv.back.controller;

import es.caib.concsv.logic.intf.config.BaseConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Informació auxiliar per al perfil de l'usuari a la interfície REACT, que no forma part del
 * motor genèric de recursos HAL-FORMS.
 *
 * @author Límit Tecnologies
 */
@RestController
@RequestMapping(BaseConfig.API_PATH + "/usuariPreferencies")
public class UsuariPreferenciesController {

	/**
	 * Mides de pàgina que pot triar l'usuari al perfil. No es publiquen com a opcions del camp
	 * {@code numElementsPagina} perquè el motor genèric lliura els valors de les opcions com a
	 * text mentre que el camp és numèric.
	 */
	@GetMapping("/opcionsPaginacio")
	public List<Opcio> opcionsPaginacio() {
		return Stream.of(10L, 20L, 50L, 100L).
				map(mida -> new Opcio(mida, String.valueOf(mida))).
				collect(Collectors.toList());
	}

	@Getter
	@AllArgsConstructor
	public static class Opcio {
		private final Long id;
		private final String nom;
	}

}
