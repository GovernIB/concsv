package es.caib.concsv.logic.intf.model;

/**
 * Carpetes de la cache de documents, dins {@code <es.caib.concsv.fitxers>/cache/}.
 * <p>
 * El nom de cada fitxer és l'identificador del document (CSV o UUID) amb els caràcters que no són
 * lletres, xifres, {@code .}, {@code _} o {@code -} substituïts per {@code _}.
 *
 * @author Límit Tecnologies
 */
public enum CacheCarpeta {

	/**
	 * Objecte {@code DocumentInfo} serialitzat, amb la informació recuperada de l'Arxiu i la
	 * informació dels signants validada amb Afirma, de manera que no s'ha de tornar a consultar
	 * l'Arxiu. Nom del fitxer: {@code UUID_<id>} o {@code CSV_<id>}.
	 */
	INFO,

	/**
	 * Document original recuperat de l'Arxiu. Nom del fitxer: l'UUID o el CSV del document.
	 */
	ORIGINAL,

	/**
	 * PDF imprimible amb els marges, per UUID o CSV i idioma. Nom del fitxer: l'UUID o el CSV del
	 * document, un guió baix i l'idioma (p. ex. {@code 2s4f5dsdf-sd54fds54f-sd45sd4f-cfsa_CA}).
	 */
	IMPRIMIBLE

}
