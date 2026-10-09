package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.DocumentExclosResource;
import es.caib.concsv.persist.base.entity.BaseAuditableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Entitat de base de dades del recurs {@link DocumentExclosResource}.
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "document_exclos")
@Getter
@Setter
@NoArgsConstructor
public class DocumentExclosEntity extends BaseAuditableEntity<DocumentExclosResource, Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "default_seq")
	@SequenceGenerator(name = "default_seq", sequenceName = BaseConfig.DB_PREFIX + "hibernate_seq", allocationSize = 1)
	private Long id;

	@Column(name = "valor", length = 256, nullable = false, unique = true)
	private String valor;

}
