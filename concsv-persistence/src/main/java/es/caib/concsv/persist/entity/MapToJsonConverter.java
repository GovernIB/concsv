package es.caib.concsv.persist.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Map;

/**
 * Converteix un {@code Map<String, String>} en JSON per desar-lo en una columna de text. Un mapa
 * buit o un JSON il·legible s'interpreten com a "sense valor" (mai provoquen error de lectura).
 *
 * @author Límit Tecnologies
 */
@Converter
public class MapToJsonConverter implements AttributeConverter<Map<String, String>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(Map<String, String> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(map);
		} catch (Exception ex) {
			return null;
		}
	}

	@Override
	public Map<String, String> convertToEntityAttribute(String json) {
		if (json == null || json.isEmpty()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
		} catch (Exception ex) {
			return null;
		}
	}

}
