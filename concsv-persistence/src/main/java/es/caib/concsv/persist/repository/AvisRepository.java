package es.caib.concsv.persist.repository;

import es.caib.concsv.persist.base.repository.BaseRepository;
import es.caib.concsv.persist.entity.AvisEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

/**
 * Repositori per a la gestió d'avisos.
 *
 * @author Límit Tecnologies
 */
public interface AvisRepository extends BaseRepository<AvisEntity, Long> {

	/**
	 * Avisos actius en una data: marcats com a actius i amb la data dins l'interval de vigència.
	 * Si s'indica una entitat només es retornen els globals (sense entitat) i els d'aquesta
	 * entitat; si no, es retornen tots.
	 */
	@Query("from AvisEntity a " +
			"where a.actiu = true " +
			"and a.dataInici <= :data " +
			"and (a.dataFinal is null or a.dataFinal >= :data) " +
			"and (:entitatId is null or a.entitat is null or a.entitat.id = :entitatId) " +
			"order by a.dataInici desc")
	List<AvisEntity> findActius(
			@Param("data") Date data,
			@Param("entitatId") Long entitatId);

	void deleteByEntitatId(Long entitatId);

}
