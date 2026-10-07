package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.ConfigGroupResource;
import es.caib.concsv.persist.base.entity.BaseResourceEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.util.Set;

/**
 * Entitat de base de dades del recurs {@link ConfigGroupResource}. La clau primària és el codi
 * del grup (clau natural, sense generador).
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "config_group")
@Getter
@Setter
@NoArgsConstructor
public class ConfigGroupEntity extends BaseResourceEntity<ConfigGroupResource, String> {

	@Id
	@Column(name = "code", length = 128, nullable = false)
	private String key;

	/** Mateixa columna que la clau primària, només lectura (veure {@link ConfigEntity}). */
	@Column(name = "code", length = 128, insertable = false, updatable = false)
	private String id;

	@Column(name = "description", length = 512)
	private String description;

	@Column(name = "position", nullable = false)
	private int position;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(
			name = "parent_code",
			foreignKey = @ForeignKey(name = BaseConfig.DB_PREFIX + "config_group_parent_fk"))
	private ConfigGroupEntity parent;

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
	@OrderBy("position ASC")
	private Set<ConfigGroupEntity> children;

	@OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
	@OrderBy("position ASC")
	private Set<ConfigEntity> configs;

	/** Veure {@link ConfigEntity#getId()}. */
	@Override
	public String getId() {
		return key;
	}

}
