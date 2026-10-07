package es.caib.concsv.persist.repository;

import es.caib.concsv.persist.base.repository.BaseRepository;
import es.caib.concsv.persist.entity.ConfigEntity;

import java.util.List;

/**
 * Repositori per a les propietats configurables.
 *
 * @author Límit Tecnologies
 */
public interface ConfigRepository extends BaseRepository<ConfigEntity, String> {

	/** Propietats que es poden modificar des del backoffice (no són dels fitxers del servidor). */
	List<ConfigEntity> findByJbossPropertyFalse();

}
