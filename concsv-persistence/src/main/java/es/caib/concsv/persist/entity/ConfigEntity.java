package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.ConfigResource;
import es.caib.concsv.persist.base.entity.BaseResourceEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Entitat de base de dades del recurs {@link ConfigResource}. La clau primària és la clau de la
 * propietat (clau natural, sense generador).
 * <p>
 * No té els camps de creació de {@code BaseAuditableEntity}: les files les crea l'script SQL de
 * la versió, i només interessa saber qui ha modificat el valor per darrera vegada.
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "config")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ConfigEntity extends BaseResourceEntity<ConfigResource, String> {

	@Id
	@Column(name = "key", length = 256, nullable = false, updatable = false)
	private String key;

	/**
	 * Mateixa columna que la clau primària, només lectura. El framework de recursos cerca la
	 * clau amb un atribut anomenat {@code id} (veure {@code PkSpec}).
	 */
	@Column(name = "key", length = 256, insertable = false, updatable = false)
	private String id;

	@Column(name = "value", length = 2048)
	private String value;

	@Column(name = "description", length = 2048, updatable = false)
	private String description;

	@Column(name = "jboss_property", nullable = false, updatable = false)
	private boolean jbossProperty;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(
			name = "group_code",
			updatable = false,
			foreignKey = @ForeignKey(name = BaseConfig.DB_PREFIX + "config_group_fk"))
	private ConfigGroupEntity group;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(
			name = "type_code",
			updatable = false,
			foreignKey = @ForeignKey(name = BaseConfig.DB_PREFIX + "config_type_fk"))
	private ConfigTypeEntity type;

	@Column(name = "position", nullable = false, updatable = false)
	private int position;

	@LastModifiedBy
	@Column(name = "lastmodifiedby_codi", length = 64)
	private String lastModifiedBy;

	@LastModifiedDate
	@Column(name = "lastmodifieddate")
	private LocalDateTime lastModifiedDate;

	/**
	 * La identitat és la clau, no l'atribut {@code id} (només lectura, buit en una fila nova). El
	 * {@code setId} és el de Lombok sobre aquest atribut: el mapeig del recurs l'hi crida amb
	 * {@code null} i no ha de tocar la clau.
	 */
	@Override
	public String getId() {
		return key;
	}

}
