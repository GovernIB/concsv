package es.caib.concsv.persist.repository;

import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.persist.base.repository.BaseRepository;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositori de les execucions massives.
 * <p>
 * Les consultes {@code update} són atòmiques i retornen el nombre de files que han canviat: així dos
 * nodes (o dues cancel·lacions) que competeixen per la mateixa execució no es trepitgen, i el que
 * guanya ho sap perquè obté 1.
 *
 * @author Límit Tecnologies
 */
public interface ExecucioMassivaRepository extends BaseRepository<ExecucioMassivaEntity, Long> {

	/**
	 * Execucions que un node pot agafar: no han acabat, ja han arribat a la data d'inici i són
	 * pendents, o bé són en curs però abandonades, és a dir, d'aquest mateix node (es reprenen en
	 * arrencar) o sense batec recent (el node que les processava ha caigut).
	 */
	String CONDICIO_PROCESSABLE =
			"e.dataFi is null and (e.dataInici is null or e.dataInici <= :ara) and (" +
			"e.estat = :pendent or (e.estat = :enCurs and (" +
			"e.node = :node or e.dataHeartbeat is null or e.dataHeartbeat < :limitHeartbeat))) ";

	/** Identificadors de les execucions que es poden agafar, de la més antiga a la més nova. */
	@Query("select e.id from ExecucioMassivaEntity e where " + CONDICIO_PROCESSABLE + "order by e.id asc")
	List<Long> findIdsProcessables(
			@Param("ara") LocalDateTime ara,
			@Param("pendent") ExecucioMassivaEstatEnum pendent,
			@Param("enCurs") ExecucioMassivaEstatEnum enCurs,
			@Param("node") String node,
			@Param("limitHeartbeat") LocalDateTime limitHeartbeat,
			Pageable pageable);

	/**
	 * Reclama una execució per a un node: la passa a {@code EN_CURS} i hi posa el node i el batec.
	 *
	 * @return 1 si l'ha reclamada, 0 si un altre node s'hi ha avançat (o ja no és processable).
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ExecucioMassivaEntity e set e.estat = :enCurs, e.node = :node, e.dataHeartbeat = :ara " +
			"where e.id = :id and " + CONDICIO_PROCESSABLE)
	int reclamar(
			@Param("id") Long id,
			@Param("ara") LocalDateTime ara,
			@Param("pendent") ExecucioMassivaEstatEnum pendent,
			@Param("enCurs") ExecucioMassivaEstatEnum enCurs,
			@Param("node") String node,
			@Param("limitHeartbeat") LocalDateTime limitHeartbeat);

	/** Renova el batec; només si l'execució continua en curs en aquest node. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ExecucioMassivaEntity e set e.dataHeartbeat = :ara " +
			"where e.id = :id and e.node = :node and e.estat = :enCurs and e.dataFi is null")
	int updateHeartbeat(
			@Param("id") Long id,
			@Param("ara") LocalDateTime ara,
			@Param("node") String node,
			@Param("enCurs") ExecucioMassivaEstatEnum enCurs);

	/**
	 * Tanca com a cancel·lada una execució que cap node no ha agafat.
	 *
	 * @return 1 si l'ha cancel·lada, 0 si ja l'havia agafat un node (o ja havia acabat).
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ExecucioMassivaEntity e set e.estat = :cancelada, e.dataFi = :ara, e.cancelacioData = :ara, " +
			"e.cancelacioUsuari = :usuari where e.id = :id and e.dataFi is null and e.estat = :pendent")
	int cancelarPendent(
			@Param("id") Long id,
			@Param("ara") LocalDateTime ara,
			@Param("usuari") String usuari,
			@Param("pendent") ExecucioMassivaEstatEnum pendent,
			@Param("cancelada") ExecucioMassivaEstatEnum cancelada);

	/**
	 * Demana la cancel·lació d'una execució en curs: el procés l'atura abans de l'element següent.
	 *
	 * @return 1 si s'ha demanat, 0 si ja havia acabat o la cancel·lació ja estava demanada.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ExecucioMassivaEntity e set e.cancelacioData = :ara, e.cancelacioUsuari = :usuari " +
			"where e.id = :id and e.dataFi is null and e.cancelacioData is null")
	int solicitarCancelacio(
			@Param("id") Long id,
			@Param("ara") LocalDateTime ara,
			@Param("usuari") String usuari);

	/** Data de cancel·lació demanada, llegida de la base de dades (mai de la sessió). */
	@Query("select e.cancelacioData from ExecucioMassivaEntity e where e.id = :id")
	LocalDateTime getCancelacioData(@Param("id") Long id);

}
