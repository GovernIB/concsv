/**
 * 
 */
package es.caib.concsv.persistence.entity;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;


/**
 * Classe basse d'on estendre per a les entitats que requereixen persistència bàsica.
 * 
 * @author Limit Tecnologies <limit@limit.es>
 */
@MappedSuperclass
public abstract class BaseEntity  {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "default_seq")
    @SequenceGenerator(name = "default_seq", sequenceName = "csv_hibernate_seq", allocationSize = 1)
    private Long id;
}
