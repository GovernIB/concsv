package es.caib.concsv.logic.intf.model;

/**
 * Estat d'una execució massiva.
 * <p>
 * {@link #EN_CURS} amb la data de cancel·lació informada vol dir que s'ha demanat la cancel·lació i
 * que el procés encara no ha aturat l'execució.
 *
 * @author Límit Tecnologies
 */
public enum ExecucioMassivaEstatEnum {
	/** Creada, encara no l'ha agafat cap node. */
	PENDENT,
	/** Un node la està processant. */
	EN_CURS,
	/** Ha acabat de processar els elements. */
	FINALITZADA,
	/** S'ha cancel·lat abans d'acabar. */
	CANCELADA
}
