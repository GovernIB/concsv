package es.caib.concsv.persist.repository;

import es.caib.concsv.logic.intf.model.IntegracioAccioEstatEnum;
import es.caib.concsv.persist.base.repository.BaseRepository;
import es.caib.concsv.persist.entity.MonitorIntegracioEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositori per al monitor d'integracions.
 *
 * @author Límit Tecnologies
 */
public interface MonitorIntegracioRepository extends BaseRepository<MonitorIntegracioEntity, Long> {

	/**
	 * Nombre de peticions d'un estat per integració. Cada fila és {@code [IntegracioCodiEnum, Long]}
	 * i només hi surten les integracions que tenen alguna petició d'aquest estat.
	 */
	@Query("select e.codi, count(e) from MonitorIntegracioEntity e where e.estat = :estat group by e.codi")
	List<Object[]> countPerCodiByEstat(@Param("estat") IntegracioAccioEstatEnum estat);

}
