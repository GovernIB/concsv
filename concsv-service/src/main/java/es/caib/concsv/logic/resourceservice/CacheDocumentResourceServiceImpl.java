package es.caib.concsv.logic.resourceservice;

import es.caib.concsv.logic.base.service.BaseNoDatabaseMutableResourceService;
import es.caib.concsv.logic.helper.CacheDocumentHelper;
import es.caib.concsv.logic.intf.base.exception.ActionExecutionException;
import es.caib.concsv.logic.intf.base.exception.AnswerRequiredException;
import es.caib.concsv.logic.intf.model.CacheDocumentResource;
import es.caib.concsv.logic.intf.resourceservice.CacheDocumentResourceService;
import es.caib.concsv.persist.base.entity.NoDatabaseResourceEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementació del servei de consulta i esborrat dels documents de la cache de documents.
 * <p>
 * Els documents no són a base de dades: cada consulta llegeix la carpeta de la cache (només el nom,
 * la mida i la data dels fitxers, veure {@link CacheDocumentHelper}), i el filtre, l'ordenació i la
 * paginació s'apliquen en memòria.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheDocumentResourceServiceImpl
		extends BaseNoDatabaseMutableResourceService<CacheDocumentResource, String>
		implements CacheDocumentResourceService {

	private static final Comparator<String> TEXT = Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER);

	/** Camps pels quals es pot ordenar el llistat. */
	private static final Map<String, Comparator<CacheDocumentResource>> ORDENACIONS = new HashMap<>();
	static {
		ORDENACIONS.put("carpeta", Comparator.comparing(r -> r.getCarpeta() != null ? r.getCarpeta().name() : null, TEXT));
		ORDENACIONS.put("identificador", Comparator.comparing(CacheDocumentResource::getIdentificador, TEXT));
		ORDENACIONS.put("tipusIdentificador", Comparator.comparing(CacheDocumentResource::getTipusIdentificador, TEXT));
		ORDENACIONS.put("idioma", Comparator.comparing(CacheDocumentResource::getIdioma, TEXT));
		ORDENACIONS.put("nomDocument", Comparator.comparing(CacheDocumentResource::getNomDocument, TEXT));
		ORDENACIONS.put("mida", Comparator.comparing(CacheDocumentResource::getMida, Comparator.nullsFirst(Comparator.naturalOrder())));
		ORDENACIONS.put("darreraConsulta", Comparator.comparing(CacheDocumentResource::getDarreraConsulta, Comparator.nullsFirst(Comparator.naturalOrder())));
		// Sense data d'expiració (la cache no caduca) va al final en ordre ascendent.
		ORDENACIONS.put("expiracio", Comparator.comparing(CacheDocumentResource::getExpiracio, Comparator.nullsLast(Comparator.naturalOrder())));
	}

	private final CacheDocumentHelper cacheDocumentHelper;

	@PostConstruct
	public void init() {
		register(CacheDocumentResource.ACTION_ESBORRAR_MASSIU_CODE, new EsborrarMassiuActionExecutor());
	}

	@Override
	protected Optional<NoDatabaseResourceEntity<CacheDocumentResource, String>> entityRepositoryFindOne(String id) {
		return cacheDocumentHelper.trobar(id).map(entrada -> entitat(entrada.toResource()));
	}

	@Override
	protected Page<NoDatabaseResourceEntity<CacheDocumentResource, String>> entityRepositoryFindEntities(
			String quickFilter,
			String filter,
			String[] namedQueries,
			Pageable pageable) {
		Predicate<CacheDocumentResource> predicat = CacheDocumentFiltre.rapid(quickFilter).and(CacheDocumentFiltre.parse(filter));
		List<CacheDocumentResource> resultat = cacheDocumentHelper.llistar().stream().
				map(CacheDocumentHelper.Entrada::toResource).
				filter(predicat).
				sorted(comparador(pageable.getSort())).
				collect(Collectors.toList());
		List<CacheDocumentResource> pagina = resultat;
		if (pageable.isPaged()) {
			int inici = (int)Math.min(pageable.getOffset(), resultat.size());
			int fi = (int)Math.min((long)inici + pageable.getPageSize(), resultat.size());
			pagina = resultat.subList(inici, fi);
		}
		return new PageImpl<>(
				pagina.stream().map(CacheDocumentResourceServiceImpl::entitat).collect(Collectors.toList()),
				pageable,
				resultat.size());
	}

	/**
	 * Ordre del llistat: el demanat (camps de {@link #ORDENACIONS}) i, si no n'hi ha, el de darrera
	 * consulta descendent. Sempre desempata per identificador perquè la paginació sigui estable.
	 */
	static Comparator<CacheDocumentResource> comparador(Sort sort) {
		Comparator<CacheDocumentResource> comparador = null;
		if (sort != null && sort.isSorted()) {
			for (Sort.Order ordre : sort) {
				Comparator<CacheDocumentResource> camp = ORDENACIONS.get(ordre.getProperty());
				if (camp == null) {
					throw new IllegalArgumentException("No es pot ordenar per " + ordre.getProperty());
				}
				camp = ordre.isAscending() ? camp : camp.reversed();
				comparador = comparador == null ? camp : comparador.thenComparing(camp);
			}
		}
		if (comparador == null) {
			comparador = ORDENACIONS.get("darreraConsulta").reversed();
		}
		return comparador.thenComparing(CacheDocumentResource::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
	}

	private static NoDatabaseResourceEntity<CacheDocumentResource, String> entitat(CacheDocumentResource resource) {
		return NoDatabaseResourceEntity.<CacheDocumentResource, String>builder().
				id(resource.getId()).
				resource(resource).
				build();
	}

	/** Esborra les entrades seleccionades. */
	private class EsborrarMassiuActionExecutor implements
			ActionExecutor<NoDatabaseResourceEntity<CacheDocumentResource, String>, CacheDocumentResource.FormEsborrarMassiu, CacheDocumentResource.ResultatEsborrat> {

		@Override
		public void onChange(
				Serializable id,
				CacheDocumentResource.FormEsborrarMassiu previous,
				String fieldName,
				Object fieldValue,
				Map<String, AnswerRequiredException.AnswerValue> answers,
				String[] previousFieldNames,
				CacheDocumentResource.FormEsborrarMassiu target) {
			// El formulari no té cap camp que depengui dels altres.
		}

		@Override
		public CacheDocumentResource.ResultatEsborrat exec(
				String code,
				NoDatabaseResourceEntity<CacheDocumentResource, String> entity,
				CacheDocumentResource.FormEsborrarMassiu params) throws ActionExecutionException {
			try {
				return cacheDocumentHelper.esborrar(params != null ? params.getIds() : new ArrayList<>());
			} catch (RuntimeException ex) {
				throw new ActionExecutionException(CacheDocumentResource.class, null, code, ex.getMessage(), ex);
			}
		}

	}

}
