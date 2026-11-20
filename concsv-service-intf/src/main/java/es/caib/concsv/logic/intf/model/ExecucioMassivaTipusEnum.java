package es.caib.concsv.logic.intf.model;

/**
 * Tipus d'una execució massiva, és a dir, de l'acció que es fa a cada un dels seus elements.
 *
 * @author Límit Tecnologies
 */
public enum ExecucioMassivaTipusEnum {

	/** Torna a consultar les dades (nom, data de captura...) dels documents exclosos. */
	ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS;

	/**
	 * Si es pot cancel·lar una execució d'aquest tipus. Ho són tots els que processen els elements
	 * un per un, ja que entre un element i el següent es pot aturar el procés.
	 */
	public boolean isCancelable() {
		return true;
	}

}
