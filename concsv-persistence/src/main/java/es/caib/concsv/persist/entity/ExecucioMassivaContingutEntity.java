package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.ElementTipusEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutResource;
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
import java.time.LocalDateTime;

/**
 * Entitat de base de dades del recurs {@link ExecucioMassivaContingutResource}: un element d'una
 * execució massiva.
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "massiva_contingut")
@Getter
@Setter
@NoArgsConstructor
public class ExecucioMassivaContingutEntity extends BaseAuditableEntity<ExecucioMassivaContingutResource, Long> {

	/** Longitud màxima de l'error desat (columna {@code ERROR}). */
	public static final int ERROR_MAX = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "default_seq")
	@SequenceGenerator(name = "default_seq", sequenceName = BaseConfig.DB_PREFIX + "hibernate_seq", allocationSize = 1)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(
			name = "execucio_massiva_id",
			nullable = false,
			foreignKey = @ForeignKey(name = BaseConfig.DB_PREFIX + "massiva_cont_exec_fk"))
	private ExecucioMassivaEntity execucioMassiva;

	@Column(name = "data_inici")
	private LocalDateTime dataInici;

	@Column(name = "data_fi")
	private LocalDateTime dataFi;

	@Enumerated(EnumType.STRING)
	@Column(name = "estat", length = 20, nullable = false)
	private ExecucioMassivaContingutEstatEnum estat = ExecucioMassivaContingutEstatEnum.PENDENT;

	@Column(name = "error", length = ERROR_MAX)
	private String error;

	@Column(name = "ordre", nullable = false)
	private int ordre;

	@Enumerated(EnumType.STRING)
	@Column(name = "element_tipus", length = 32, nullable = false)
	private ElementTipusEnum elementTipus;

	@Column(name = "element_id", nullable = false)
	private Long elementId;

	@Column(name = "element_nom", length = 256)
	private String elementNom;

	/** L'element s'ha tractat bé. */
	public void updateFinalitzat(LocalDateTime dataFi) {
		this.estat = ExecucioMassivaContingutEstatEnum.FINALITZAT;
		this.dataFi = dataFi;
	}

	/** El tractament de l'element ha fallat; l'error es retalla a la mida de la columna. */
	public void updateError(LocalDateTime dataFi, String error) {
		this.estat = ExecucioMassivaContingutEstatEnum.ERROR;
		this.dataFi = dataFi;
		this.error = error != null && error.length() > ERROR_MAX ? error.substring(0, ERROR_MAX) : error;
	}

}
