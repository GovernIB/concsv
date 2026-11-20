package es.caib.concsv.logic.intf.model;

/**
 * Estat d'un element d'una execució massiva.
 *
 * @author Límit Tecnologies
 */
public enum ExecucioMassivaContingutEstatEnum {
	/** Encara no s'ha tractat. */
	PENDENT,
	/** S'ha tractat sense error. */
	FINALITZAT,
	/** El tractament ha fallat; el motiu és al camp d'error de l'element. */
	ERROR,
	/** No s'ha arribat a tractar perquè l'execució s'ha cancel·lat. */
	CANCELAT
}
