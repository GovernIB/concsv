package es.caib.concsv.persist.repository;

import es.caib.concsv.logic.intf.model.ElementTipusEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.persist.base.repository.BaseRepository;
import es.caib.concsv.persist.entity.ExecucioMassivaContingutEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Repositori dels elements de les execucions massives.
 *
 * @author Límit Tecnologies
 */
public interface ExecucioMassivaContingutRepository extends BaseRepository<ExecucioMassivaContingutEntity, Long> {

	/** Identificadors dels elements d'una execució en un estat (p. ex. els pendents), en ordre de tractament. */
	@Query("select c.id from ExecucioMassivaContingutEntity c " +
			"where c.execucioMassiva.id = :execucioId and c.estat = :estat order by c.ordre asc")
	List<Long> findIdsPerEstat(
			@Param("execucioId") Long execucioId,
			@Param("estat") ExecucioMassivaContingutEstatEnum estat);

	/** Marca com a error, amb el mateix motiu, els elements que encara no s'han tractat. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ExecucioMassivaContingutEntity c set c.estat = :estatError, c.dataFi = :ara, c.error = :error " +
			"where c.execucioMassiva.id = :execucioId and c.estat = :pendent")
	int errorPendents(
			@Param("execucioId") Long execucioId,
			@Param("ara") LocalDateTime ara,
			@Param("error") String error,
			@Param("pendent") ExecucioMassivaContingutEstatEnum pendent,
			@Param("estatError") ExecucioMassivaContingutEstatEnum estatError);

	/** Cancel·la de cop els elements que encara no s'han tractat. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update ExecucioMassivaContingutEntity c set c.estat = :cancelat, c.dataFi = :ara " +
			"where c.execucioMassiva.id = :execucioId and c.estat = :pendent")
	int cancelarPendents(
			@Param("execucioId") Long execucioId,
			@Param("ara") LocalDateTime ara,
			@Param("pendent") ExecucioMassivaContingutEstatEnum pendent,
			@Param("cancelat") ExecucioMassivaContingutEstatEnum cancelat);

	/**
	 * Nombre d'elements d'una execució per estat. Cada fila és {@code [ExecucioMassivaContingutEstatEnum, Long]}
	 * i només hi surten els estats que tenen algun element.
	 */
	@Query("select c.estat, count(c) from ExecucioMassivaContingutEntity c " +
			"where c.execucioMassiva.id = :execucioId group by c.estat")
	List<Object[]> countPerEstat(@Param("execucioId") Long execucioId);

	/**
	 * D'entre els elements indicats, els que ja són pendents en una execució oberta del mateix tipus:
	 * no s'han de tornar a afegir a una altra execució.
	 */
	@Query("select distinct c.elementId from ExecucioMassivaContingutEntity c " +
			"where c.elementTipus = :elementTipus and c.elementId in :elementIds " +
			"and c.estat = :pendent and c.execucioMassiva.tipus = :tipus and c.execucioMassiva.dataFi is null")
	List<Long> findElementIdsEnExecucioOberta(
			@Param("elementTipus") ElementTipusEnum elementTipus,
			@Param("elementIds") Collection<Long> elementIds,
			@Param("tipus") ExecucioMassivaTipusEnum tipus,
			@Param("pendent") ExecucioMassivaContingutEstatEnum pendent);

}
