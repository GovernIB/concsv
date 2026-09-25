package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.MenuEstilEnum;
import es.caib.concsv.logic.intf.model.TemaAplicacioEnum;
import es.caib.concsv.logic.intf.model.UsuariResource;
import es.caib.concsv.persist.base.entity.BaseResourceEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

/**
 * Entitat de base de dades del recurs {@link UsuariResource}. La clau primària és el codi de
 * l'usuari (clau natural, sense generador).
 *
 * @author Límit Tecnologies
 */
@Entity
@Table(name = BaseConfig.DB_PREFIX + "usuari")
@Getter
@Setter
@NoArgsConstructor
public class UsuariEntity extends BaseResourceEntity<UsuariResource, String> {

	@Id
	@Column(name = "codi", length = 64, nullable = false)
	private String id;

	@Column(name = "nom", length = 200)
	private String nom;

	@Column(name = "nif", length = 9)
	private String nif;

	@Column(name = "email", length = 200)
	private String email;

	@Column(name = "idioma", length = 2, nullable = false)
	private String idioma = "CA";

	/** Sense clau forana a posta: si s'esborra l'entitat la preferència simplement deixa de valer. */
	@Column(name = "entitat_actual_id")
	private Long entitatActualId;

	@Column(name = "num_elements_pagina")
	private Long numElementsPagina;

	@Column(name = "rol_actual", length = 64)
	private String rolActual;

	@Enumerated(EnumType.STRING)
	@Column(name = "tema_aplicacio", length = 16)
	private TemaAplicacioEnum temaAplicacio;

	@Enumerated(EnumType.STRING)
	@Column(name = "estil_menu", length = 16, nullable = false)
	private MenuEstilEnum estilMenu = MenuEstilEnum.TEMA;

	@Version
	private long version = 0;

}
