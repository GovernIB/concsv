package es.caib.concsv.persist.entity;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.DocumentExclosEstatEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosOrigenEnum;
import es.caib.concsv.logic.intf.model.DocumentExclosResource;
import es.caib.concsv.logic.intf.model.DocumentExclosUbicacioEnum;
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
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

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

	// Dades del document, llegides de l'Arxiu Digital o de la custòdia antiga: tot és opcional.

	@Column(name = "nom_document", length = 512)
	private String nomDocument;

	@Column(name = "data_captura")
	private LocalDateTime dataCaptura;

	@Column(name = "csv", length = 256)
	private String csv;

	@Column(name = "uuid", length = 64)
	private String uuid;

	@Column(name = "organ", length = 256)
	private String organ;

	@Convert(converter = DocumentExclosOrigenConverter.class)
	@Column(name = "origen", length = 64)
	private DocumentExclosOrigenEnum origen;

	@Enumerated(EnumType.STRING)
	@Column(name = "ubicacio", length = 32)
	private DocumentExclosUbicacioEnum ubicacio;

	@Column(name = "data_actualitzacio")
	private LocalDateTime dataActualitzacio;

	/** Resultat de la darrera consulta; {@code PENDENT} mentre no s'ha consultat mai. */
	@Enumerated(EnumType.STRING)
	@Column(name = "estat_actualitzacio", length = 16, nullable = false)
	private DocumentExclosEstatEnum estatActualitzacio = DocumentExclosEstatEnum.PENDENT;

	@Column(name = "missatge_actualitzacio", length = 1000)
	private String missatgeActualitzacio;

}
