package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.AvisNivellEnum;
import es.caib.concsv.logic.intf.model.AvisResource;
import es.caib.concsv.persist.base.entity.BaseAuditableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

/**
 * Entitat de base de dades del recurs {@link AvisResource}.
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "avis")
@Getter
@Setter
@NoArgsConstructor
public class AvisEntity extends BaseAuditableEntity<AvisResource, Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "default_seq")
	@SequenceGenerator(name = "default_seq", sequenceName = BaseConfig.DB_PREFIX + "hibernate_seq", allocationSize = 1)
	private Long id;

	@Column(name = "assumpte", length = 256, nullable = false)
	private String assumpte;

	@Column(name = "missatge", length = 2048, nullable = false)
	private String missatge;

	@Temporal(TemporalType.DATE)
	@Column(name = "data_inici", nullable = false)
	private Date dataInici;

	@Temporal(TemporalType.DATE)
	@Column(name = "data_final")
	private Date dataFinal;

	@Column(name = "actiu", nullable = false)
	private Boolean actiu;

	@Enumerated(EnumType.STRING)
	@Column(name = "avis_nivell", length = 10, nullable = false)
	private AvisNivellEnum avisNivell;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "entitat_id",
			foreignKey = @ForeignKey(name = BaseConfig.DB_PREFIX + "avis_entitat_fk"))
	private EntitatEntity entitat;

}
