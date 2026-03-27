package es.caib.concsv.ejb.helpers;

import es.caib.concsv.persistence.entity.ExplotDimensioEntity;
import es.caib.concsv.persistence.entity.ExplotFetsEntity;
import es.caib.concsv.persistence.entity.ExplotTempsEntity;
import es.caib.concsv.persistence.model.EnviamentOrigen;
import es.caib.concsv.persistence.model.EnviamentTipus;
import es.caib.concsv.service.enums.ResultTypeEnum;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

@Slf4j
@ApplicationScoped
@Getter
public class EstadisticaHelper {

    @PersistenceContext(unitName = "concsvPU")
    private EntityManager entityManager;
    @Inject @ConfigProperty(name = "es.caib.concsv.estadisticas.dias.conservar", defaultValue = "365")
    private int diasConservar;

    /** Comptadors d'esdeveniments per dimensió i tipus de resultat */
    private final Map<String, EnumMap<ResultTypeEnum, LongAdder>> counters = new ConcurrentHashMap<>();

    /** Acumulador de temps només per als OK */
    private final Map<String, LongAdder> timeAccumulators = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.debug("EstadisticaHelper initialized");
    }

    /** Registra un esdeveniment en memòria */
    public void register(String key, ResultTypeEnum result, long durationMillis) {
        counters.computeIfAbsent(key, k -> {
            EnumMap<ResultTypeEnum, LongAdder> map = new EnumMap<>(ResultTypeEnum.class);
            map.put(ResultTypeEnum.OK, new LongAdder());
            map.put(ResultTypeEnum.INVALID, new LongAdder());
            map.put(ResultTypeEnum.ERROR, new LongAdder());
            return map;
        });

        counters.get(key).get(result).increment();

        if (result == ResultTypeEnum.OK) {
            timeAccumulators.computeIfAbsent(key, k -> new LongAdder()).add(durationMillis);
        }
    }

    /** Persisteix les dades acumulades a la base de dades i reinicia els comptadors */
    @Transactional
    public void flushToDatabase(LocalDate date) {
        ExplotTempsEntity temps = findFirstTempsByData(date).orElseGet(() -> save(new ExplotTempsEntity(date)));

        counters.forEach((key, map) -> {
            String[] parts = key.split(":");
            EnviamentTipus tipus = Enum.valueOf(EnviamentTipus.class, parts[0]);
            EnviamentOrigen origen = Enum.valueOf(EnviamentOrigen.class, parts[1]);
            ExplotDimensioEntity dim = findDimensioByTipusAndOrigen(tipus, origen)
            .orElseGet(() -> {
                log.info("Dimensio no trobada per tipus {} i origen {}, es crearà automàticament", tipus, origen);
                return save(ExplotDimensioEntity.builder().tipus(tipus).origen(origen).build());
            });

            long newCorrectes = map.get(ResultTypeEnum.OK).longValue();
            long newInvalids  = map.get(ResultTypeEnum.INVALID).longValue();
            long newErrors    = map.get(ResultTypeEnum.ERROR).longValue();
            long newTotalTime = timeAccumulators.getOrDefault(key, new LongAdder()).longValue();

            ExplotFetsEntity existing = findFetByDimensioAndTemps(dim, temps).orElse(null);

            if (existing == null) {
                long avg = (newCorrectes > 0) ? newTotalTime / newCorrectes : 0L;
                ExplotFetsEntity entity = ExplotFetsEntity.builder()
                        .dimensio(dim)
                        .temps(temps)
                        .correcte(newCorrectes)
                        .codiInvalid(newInvalids)
                        .error(newErrors)
                        .tempsMigCorrecte(avg)
                        .build();
                save(entity);
            } else {
                long oldCorrectes = existing.getCorrecte() != null ? existing.getCorrecte() : 0L;
                long oldInvalids  = existing.getCodiInvalid() != null ? existing.getCodiInvalid() : 0L;
                long oldErrors    = existing.getError() != null ? existing.getError() : 0L;
                long oldAvg       = existing.getTempsMigCorrecte() != null ? existing.getTempsMigCorrecte() : 0L;

                long totalCorrectes = oldCorrectes + newCorrectes;

                long newAvg = 0L;
                if (totalCorrectes > 0) {
                    long oldTotalTime = oldCorrectes * oldAvg;
                    newAvg = (oldTotalTime + newTotalTime) / totalCorrectes;
                }

                existing.setCorrecte(totalCorrectes);
                existing.setCodiInvalid(oldInvalids + newInvalids);
                existing.setError(oldErrors + newErrors);
                existing.setTempsMigCorrecte(newAvg);

                save(existing);
            }

            // Reiniciamos contadores
            map.values().forEach(LongAdder::reset);
            timeAccumulators.remove(key);
        });

        log.debug("EstadisticaHelper flushed/updated to database for date {}", date);
    }

    /** Esborra tots els Temps i Fets anteriors a la data límit */
    @Transactional
    public void netejarEstadistiques(LocalDate date) {
        LocalDate fechaLimite = date.minusDays(diasConservar);
        List<ExplotTempsEntity> antiguos = findAllTempsBefore(fechaLimite);

        for (ExplotTempsEntity t : antiguos) {
            deleteFetsByTemps(t);
            entityManager.remove(t);
        }

        log.info("Esborrades {} estadístiques anteriors a {}", antiguos.size(), fechaLimite);
    }

    /**
     * Comprueba si la base de datos está operativa midiendo la latencia.
     * @return Latencia en milisegundos si la conexión es correcta, o null si hay error.
     */
    public Long checkDatabaseLatency() {
        Instant start = Instant.now();
        try {
            entityManager.createQuery("SELECT 1 FROM ExplotDimensioEntity e")
                    .setMaxResults(1)
                    .getResultList();
            return Duration.between(start, Instant.now()).toMillis();
        } catch (Exception e) {
            return null; // error de conexión o de ejecución
        }
    }

    /* -------------------- Persitencia (DAO) --------------------*/

    /* ---------- ExplotTempsEntity ---------- */

    public Optional<ExplotTempsEntity> findFirstTempsByData(LocalDate date) {
        try {
            ExplotTempsEntity result = entityManager.createQuery(
                            "SELECT t FROM ExplotTempsEntity t WHERE t.data = :date", ExplotTempsEntity.class)
                    .setParameter("date", date)
                    .setMaxResults(1)
                    .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /** Troba tots els ExplotTempsEntity anteriors a una data */
    public List<ExplotTempsEntity> findAllTempsBefore(LocalDate fechaLimite) {
        try {
            return entityManager.createQuery(
                            "SELECT t FROM ExplotTempsEntity t WHERE t.data < :fechaLimite",
                            ExplotTempsEntity.class)
                    .setParameter("fechaLimite", fechaLimite)
                    .getResultList();
        } catch (NoResultException e) {
            return List.of();
        }
    }

    /* ---------- ExplotDimensioEntity ---------- */

    public Optional<ExplotDimensioEntity> findDimensioByTipusAndOrigen(EnviamentTipus tipus, EnviamentOrigen origen) {
        try {
            ExplotDimensioEntity result = entityManager.createQuery(
                            "SELECT d FROM ExplotDimensioEntity d WHERE d.tipus = :tipus AND d.origen = :origen",
                            ExplotDimensioEntity.class)
                    .setParameter("tipus", tipus)
                    .setParameter("origen", origen)
                    .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    /* ---------- ExplotFetsEntity ---------- */

    public Optional<ExplotFetsEntity> findFetByDimensioAndTemps(ExplotDimensioEntity dim, ExplotTempsEntity temps) {
        try {
            ExplotFetsEntity result = entityManager.createQuery(
                            "SELECT f FROM ExplotFetsEntity f WHERE f.dimensio = :dimensio AND f.temps = :temps",
                            ExplotFetsEntity.class)
                    .setParameter("dimensio", dim)
                    .setParameter("temps", temps)
                    .getSingleResult();
            return Optional.of(result);
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<ExplotFetsEntity> findFetByTemps(ExplotTempsEntity temps) {
        try {
            return entityManager.createQuery(
                            "SELECT f FROM ExplotFetsEntity f WHERE f.temps = :temps", ExplotFetsEntity.class)
                    .setParameter("temps", temps)
                    .getResultList();
        } catch (NoResultException e) {
            return List.of();
        }
    }

    /** Troba totes les Fets associades a un Temps */
    public List<ExplotFetsEntity> findFetsByTemps(ExplotTempsEntity temps) {
        try {
            return entityManager.createQuery(
                            "SELECT f FROM ExplotFetsEntity f WHERE f.temps = :temps",
                            ExplotFetsEntity.class)
                    .setParameter("temps", temps)
                    .getResultList();
        } catch (NoResultException e) {
            return List.of();
        }
    }

    /** Esborra totes les Fets associades a un Temps */
    @Transactional
    public void deleteFetsByTemps(ExplotTempsEntity temps) {
        List<ExplotFetsEntity> fets = findFetsByTemps(temps);
        for (ExplotFetsEntity f : fets) {
            entityManager.remove(f);
        }
    }

    /** Persiste cualquier entidad */
    @Transactional
    public <T> T save(T entity) {
        if (!entityManager.contains(entity)) {
            entityManager.persist(entity);
            return entity;
        } else {
            return entityManager.merge(entity);
        }
    }
}
