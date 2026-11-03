package es.caib.concsv.logic.intf.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.InputStream;

/**
 * Fitxer de log preparat per enviar-lo en streaming, sense carregar-lo a memòria.
 *
 * <p>Qui el rep ha de tancar {@link #getInputStream()}: si el fitxer s'ha comprimit a un temporal,
 * el temporal s'esborra en tancar-lo. Només es pot passar per interfícies locals (no és serialitzable).
 */
@Getter
@AllArgsConstructor
public class FitxerStream {

	private final InputStream inputStream;
	private final String nom;
	/** Mida en bytes del que es llegirà de {@link #inputStream} (el zip, si s'ha comprimit). */
	private final long mida;
	private final String mimeType;

}
