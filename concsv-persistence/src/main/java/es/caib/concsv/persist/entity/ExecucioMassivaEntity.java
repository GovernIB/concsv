package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaResource;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.persist.base.entity.BaseAuditableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat de base de dades del recurs {@link ExecucioMassivaResource}.
 * <p>
 * {@link DynamicUpdate}: l'estat, la cancel·lació i el batec els modifiquen altres transaccions
 * (consultes {@code update} del repositori) mentre el procés en segon pla té l'entitat carregada.
 * Sense això, desar-la tornaria a escriure tots els camps amb els valors que va llegir.
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "execucio_massiva")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
public class ExecucioMassivaEntity extends BaseAuditableEntity<ExecucioMassivaResource, Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "default_seq")
	@SequenceGenerator(name = "default_seq", sequenceName = BaseConfig.DB_PREFIX + "hibernate_seq", allocationSize = 1)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipus", length = 64, nullable = false)
	private ExecucioMassivaTipusEnum tipus;

	/** A partir d'aquesta data el procés en segon pla pot agafar l'execució. */
	@Column(name = "data_inici")
	private LocalDateTime dataInici;

	/** Data de finalització; {@code null} mentre l'execució no ha acabat. */
	@Column(name = "data_fi")
	private LocalDateTime dataFi;

	@Column(name = "enviar_correu", nullable = false)
	private boolean enviarCorreu;

	@Enumerated(EnumType.STRING)
	@Column(name = "estat", length = 20, nullable = false)
	private ExecucioMassivaEstatEnum estat;

	@Column(name = "cancelacio_data")
	private LocalDateTime cancelacioData;

	@Column(name = "cancelacio_usuari", length = 64)
	private String cancelacioUsuari;

	/** Node del servidor que l'està processant. */
	@Column(name = "node", length = 255)
	private String node;

	/** Darrer senyal de vida del node que l'està processant. */
	@Column(name = "data_heartbeat")
	private LocalDateTime dataHeartbeat;

	@PrePersist
	private void estatInicial() {
		if (estat == null) {
			estat = ExecucioMassivaEstatEnum.PENDENT;
		}
	}

}
