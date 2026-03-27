
package es.caib.concsv.persistence.model;

import java.io.Serializable;

/**
 * Enumerat que indica el tipus d'enviament de la petició.
 *
 * @author Limit Tecnologies <limit@limit.es>
 */
public enum  EnviamentTipus implements Serializable {
    IMPRIMIBLE,
    ORIGINAL,
    ENI,
    METADATOS
}
