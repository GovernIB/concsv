package es.caib.concsv.persist.repository;

import es.caib.concsv.persist.base.repository.BaseRepository;
import es.caib.concsv.persist.entity.EntitatEntity;

import java.util.List;

/**
 * Repositori per a la gestió d'entitats.
 *
 * @author Límit Tecnologies
 */
public interface EntitatRepository extends BaseRepository<EntitatEntity, Long> {

	List<EntitatEntity> findByActivaTrueOrderByNomAsc();

}
