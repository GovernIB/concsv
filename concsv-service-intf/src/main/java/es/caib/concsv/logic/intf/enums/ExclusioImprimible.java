package es.caib.concsv.logic.intf.enums;

import java.util.Optional;

/**
 * Valors de la propietat {@code es.caib.concsv.imprimible.exclusio}: quins PDF s'exclouen de la
 * generació de la còpia autèntica imprimible (issue #18). D'aquests documents només es pot
 * descarregar l'original, perquè la còpia imprimible només conté les pàgines del PDF i en perdria
 * els documents incrustats.
 * <p>
 * El codi de cada valor és el que es desa a la propietat i el que llista el tipus
 * {@code IMPRIMIBLE_EXCLUSIO} de la taula {@code CSV_CONFIG_TYPE}.
 *
 * @author Límit Tecnologies
 */
public enum ExclusioImprimible {

	/** No s'exclou cap PDF (comportament anterior a la 2.2.2). */
	DESACTIVAT,
	/** S'exclouen els PDF portafoli (catàleg amb l'entrada {@code /Collection}). */
	PORTAFOLI,
	/** S'exclouen els PDF portafoli i els PDF que contenen fitxers adjunts. */
	PORTAFOLI_ADJUNTS;

	/** Valor que s'aplica si la propietat no està definida o no és vàlida. */
	public static final ExclusioImprimible PER_DEFECTE = PORTAFOLI;

	/**
	 * Indica si s'exclou de la còpia imprimible un PDF amb les característiques indicades.
	 *
	 * @param portafoli      si el PDF és un portafoli.
	 * @param fitxersAdjunts si el PDF conté fitxers adjunts (un portafoli també en conté).
	 */
	public boolean exclou(boolean portafoli, boolean fitxersAdjunts) {
		switch (this) {
			case PORTAFOLI:
				return portafoli;
			case PORTAFOLI_ADJUNTS:
				return portafoli || fitxersAdjunts;
			default:
				return false;
		}
	}

	/**
	 * Valor corresponent al text de la propietat, sense distingir majúscules ni espais als extrems.
	 *
	 * @return el valor, o buit si el text és nul, buit o no correspon a cap valor.
	 */
	public static Optional<ExclusioImprimible> fromValor(String valor) {
		if (valor == null || valor.trim().isEmpty()) {
			return Optional.empty();
		}
		String codi = valor.trim().toUpperCase();
		for (ExclusioImprimible exclusio : values()) {
			if (exclusio.name().equals(codi)) {
				return Optional.of(exclusio);
			}
		}
		return Optional.empty();
	}

}
