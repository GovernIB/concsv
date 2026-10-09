package es.caib.concsv.persist.repository;

import es.caib.concsv.persist.base.repository.BaseRepository;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Repositori per a la gestió dels documents exclosos de la descàrrega de l'original.
 *
 * @author Límit Tecnologies
 */
public interface DocumentExclosRepository extends BaseRepository<DocumentExclosEntity, Long> {

	/** Tots els valors, ordenats (per a l'exportació i per detectar els ja existents en importar). */
	@Query("select d.valor from DocumentExclosEntity d order by d.valor")
	List<String> findAllValors();

}
