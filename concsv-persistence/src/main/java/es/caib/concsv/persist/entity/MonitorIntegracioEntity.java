package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.logic.intf.model.IntegracioAccioTipusEnum;
import es.caib.concsv.logic.intf.model.IntegracioCodiEnum;
import es.caib.concsv.logic.intf.model.MonitorIntegracioResource;
import es.caib.concsv.persist.base.entity.BaseAuditableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.Map;

/**
 * Entitat de base de dades del recurs {@link MonitorIntegracioResource}.
 * <p>
 * Els registres no els escriu el backoffice (Spring) sinó la capa de negoci de l'aplicació
 * pública, per JDBC: aquesta entitat només serveix per llegir-los.
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "integracio_accio")
@Getter
@Setter
@NoArgsConstructor
public class MonitorIntegracioEntity extends BaseAuditableEntity<MonitorIntegracioResource, Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "default_seq")
	@SequenceGenerator(name = "default_seq", sequenceName = BaseConfig.DB_PREFIX + "hibernate_seq", allocationSize = 1)
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "data", nullable = false)
	private Date data;

	@Enumerated(EnumType.STRING)
	@Column(name = "integracio_codi", length = 64, nullable = false)
	private IntegracioCodiEnum codi;

	@Column(name = "descripcio", length = 1024)
	private String descripcio;

	@Column(name = "endpoint", length = 1024)
	private String endpoint;

	@Lob
	@Convert(converter = MapToJsonConverter.class)
	@Column(name = "parametres")
	private Map<String, String> parametres;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipus", length = 32)
	private IntegracioAccioTipusEnum tipus;

	@Column(name = "temps_resposta")
	private Long tempsResposta;

	@Enumerated(EnumType.STRING)
	@Column(name = "estat", length = 16, nullable = false)
	private IntegracioAccioEstatEnum estat;

	@Column(name = "entitat_codi", length = 64)
	private String entitatCodi;

	@Column(name = "error_descripcio", length = 2000)
	private String errorDescripcio;

	@Column(name = "excepcio_message", length = 2000)
	private String excepcioMessage;

	@Lob
	@Column(name = "excepcio_stacktrace")
	private String excepcioStacktrace;

}
