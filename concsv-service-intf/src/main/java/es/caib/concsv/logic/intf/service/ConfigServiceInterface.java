package es.caib.concsv.logic.intf.service;

/**
 * Accés de les aplicacions web a les propietats que es poden canviar des del backoffice.
 */
public interface ConfigServiceInterface {

	/**
	 * Valor d'una propietat: el de la base de dades si n'hi ha (propietats editables) i, si no, el
	 * del fitxer de propietats del servidor.
	 *
	 * @param key
	 *            la clau de la propietat.
	 * @return el valor, o {@code null} si no està definida enlloc.
	 */
	String getProperty(String key);

}
