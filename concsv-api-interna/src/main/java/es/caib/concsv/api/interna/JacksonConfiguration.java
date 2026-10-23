package es.caib.concsv.api.interna;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

/**
 * {@link ObjectMapper} del proveïdor JSON de l'API interna.
 * <p>
 * Els models de Comanda fan servir {@code java.time} ({@code OffsetDateTime} a {@code AppInfo},
 * {@code SalutInfo} i {@code RegistresEstadistics}). Sense el {@link JavaTimeModule} Jackson
 * falla en arribar a la primera data, quan la resposta ja s'ha començat a escriure, i el client
 * rep el JSON tallat. Les dates s'escriuen en format ISO-8601.
 */
@Provider
public class JacksonConfiguration implements ContextResolver<ObjectMapper> {

	private final ObjectMapper objectMapper = new ObjectMapper()
		.registerModule(new JavaTimeModule())
		.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	@Override
	public ObjectMapper getContext(Class<?> type) {
		return objectMapper;
	}

}
