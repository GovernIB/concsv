package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.ConfigTypeResource;
import es.caib.concsv.persist.base.entity.BaseResourceEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Entitat de base de dades del recurs {@link ConfigTypeResource}. La clau primària és el codi del
 * tipus (clau natural, sense generador).
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "config_type")
@Getter
@Setter
@NoArgsConstructor
public class ConfigTypeEntity extends BaseResourceEntity<ConfigTypeResource, String> {

	@Id
	@Column(name = "code", length = 128, nullable = false)
	private String code;

	/** Mateixa columna que la clau primària, només lectura (veure {@link ConfigEntity}). */
	@Column(name = "code", length = 128, insertable = false, updatable = false)
	private String id;

	@Column(name = "value", length = 2048)
	private String value;

	/** Valors vàlids quan el tipus és una enumeració (llista separada per comes). */
	public List<String> getValidValues() {
		if (value == null || value.isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.asList(value.split(","));
	}

	/** Veure {@link ConfigEntity#getId()}. */
	@Override
	public String getId() {
		return code;
	}

}
