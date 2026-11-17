package es.caib.concsv.logic.intf.model;

/**
 * Estat de les dades d'un document exclòs ({@link DocumentExclosResource#getEstatActualitzacio()}).
 * {@link #PENDENT} és el document que encara no s'ha consultat; la resta són el resultat de la darrera consulta.
 *
 * @author Límit Tecnologies
 */
public enum DocumentExclosEstatEnum {
	PENDENT,
	OK,
	NO_TROBAT,
	ERROR
}
