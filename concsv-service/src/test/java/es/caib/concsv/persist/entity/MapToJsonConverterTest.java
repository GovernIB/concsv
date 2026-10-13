package es.caib.concsv.persist.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Tests del conversor que desa els paràmetres de les peticions d'integració com a JSON.
 *
 * <p>Està en aquest mòdul perquè concsv-persistence no té tests.</p>
 */
public class MapToJsonConverterTest {

	private final MapToJsonConverter converter = new MapToJsonConverter();

	@Test
	public void mapaNulIBuitNoEsDesen() {
		assertNull(converter.convertToDatabaseColumn(null));
		assertNull(converter.convertToDatabaseColumn(Collections.emptyMap()));
	}

	@Test
	public void jsonNulIBuitNoDonenCapMapa() {
		assertNull(converter.convertToEntityAttribute(null));
		assertNull(converter.convertToEntityAttribute(""));
	}

	@Test
	public void viatgeDAnadaITornada() {
		Map<String, String> parametres = new LinkedHashMap<>();
		parametres.put("hash", "ab12");
		parametres.put("uuid", "d3c4-\"cometes\"-àèí");

		String json = converter.convertToDatabaseColumn(parametres);

		assertEquals(parametres, converter.convertToEntityAttribute(json));
	}

	@Test
	public void jsonIl_legibleNoProvocaError() {
		assertNull(converter.convertToEntityAttribute("no és json"));
	}

}
