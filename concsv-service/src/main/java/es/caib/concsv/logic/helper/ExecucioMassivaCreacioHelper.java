package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.model.ElementTipusEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaContingutEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.repository.DocumentExclosRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creació de les execucions massives (equivalent a {@code crearExecucioMassiva} de RIPEA).
 * <p>
 * És una classe a part de {@link ExecucioMassivaHelper} perquè els serveis dels recursos que
 * l'originen (p. ex. els documents exclosos) l'usen, i aquell depèn d'ells per tractar els elements:
 * junts farien una dependència circular.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecucioMassivaCreacioHelper {

	/** Oracle no admet més de 1000 elements a un {@code IN}. */
	static final int MIDA_BLOC_IN = 500;

	private final ExecucioMassivaRepository execucioMassivaRepository;
	private final ExecucioMassivaContingutRepository contingutRepository;
	private final DocumentExclosRepository documentExclosRepository;

	/** Resultat de {@link #crea}. */
	public static class ResultatCreacio {
		/** Execució creada, o {@code null} si no ha quedat cap element a tractar. */
		public final Long execucioId;
		/** Elements que s'han afegit a l'execució. */
		public final int total;
		/** Elements que s'han deixat fora: repetits, ja pendents en una altra execució o que ja no existeixen. */
		public final int omesos;

		public ResultatCreacio(Long execucioId, int total, int omesos) {
			this.execucioId = execucioId;
			this.total = total;
			this.omesos = omesos;
		}
	}

	/**
	 * Crea una execució massiva, pendent de ser agafada pel procés en segon pla, amb un element per
	 * cada identificador. Els repetits, els que ja són pendents en una altra execució oberta del
	 * mateix tipus i els que ja no existeixen no s'hi afegeixen (es compten a {@code omesos}). Si no en
	 * queda cap no es crea res.
	 * <p>
	 * L'usuari creador és l'autenticat (ho registra l'auditoria) i és el que després rep el correu.
	 */
	@Transactional
	public ResultatCreacio crea(
			ExecucioMassivaTipusEnum tipus,
			ElementTipusEnum elementTipus,
			Collection<Long> ids,
			boolean enviarCorreu) {
		Set<Long> sensePendents = ids == null ? new LinkedHashSet<>() : new LinkedHashSet<>(ids);
		sensePendents.remove(null);
		int repetits = (ids == null ? 0 : ids.size()) - sensePendents.size();
		List<Long> candidats = new ArrayList<>(sensePendents);

		Set<Long> jaPendents = new HashSet<>();
		for (List<Long> bloc : blocs(candidats)) {
			jaPendents.addAll(contingutRepository.findElementIdsEnExecucioOberta(
					elementTipus,
					bloc,
					tipus,
					ExecucioMassivaContingutEstatEnum.PENDENT));
		}
		List<Long> aTractar = new ArrayList<>();
		for (Long id : candidats) {
			if (!jaPendents.contains(id)) {
				aTractar.add(id);
			}
		}
		Map<Long, String> noms = noms(elementTipus, aTractar);
		aTractar.removeIf(id -> !noms.containsKey(id));

		int omesos = repetits + candidats.size() - aTractar.size();
		if (aTractar.isEmpty()) {
			return new ResultatCreacio(null, 0, omesos);
		}

		ExecucioMassivaEntity execucio = new ExecucioMassivaEntity();
		execucio.setTipus(tipus);
		execucio.setDataInici(LocalDateTime.now());
		execucio.setEnviarCorreu(enviarCorreu);
		execucio.setEstat(ExecucioMassivaEstatEnum.PENDENT);
		execucioMassivaRepository.save(execucio);

		List<ExecucioMassivaContingutEntity> continguts = new ArrayList<>(aTractar.size());
		int ordre = 0;
		for (Long id : aTractar) {
			ExecucioMassivaContingutEntity contingut = new ExecucioMassivaContingutEntity();
			contingut.setExecucioMassiva(execucio);
			contingut.setElementTipus(elementTipus);
			contingut.setElementId(id);
			contingut.setElementNom(noms.get(id));
			contingut.setOrdre(ordre++);
			contingut.setEstat(ExecucioMassivaContingutEstatEnum.PENDENT);
			continguts.add(contingut);
		}
		contingutRepository.saveAll(continguts);
		log.info("Creada l'execució massiva {} ({}) amb {} elements ({} omesos)",
				execucio.getId(), tipus, continguts.size(), omesos);
		return new ResultatCreacio(execucio.getId(), continguts.size(), omesos);
	}

	/** Noms dels elements que existeixen, de tots de cop (no una consulta per element). */
	private Map<Long, String> noms(ElementTipusEnum elementTipus, List<Long> ids) {
		Map<Long, String> noms = new HashMap<>();
		if (elementTipus == ElementTipusEnum.DOCUMENT) {
			for (List<Long> bloc : blocs(ids)) {
				for (DocumentExclosEntity document : documentExclosRepository.findAllById(bloc)) {
					noms.put(document.getId(), document.getValor());
				}
			}
		}
		return noms;
	}

	private static <T> List<List<T>> blocs(List<T> llista) {
		List<List<T>> blocs = new ArrayList<>();
		for (int i = 0; i < llista.size(); i += MIDA_BLOC_IN) {
			blocs.add(llista.subList(i, Math.min(i + MIDA_BLOC_IN, llista.size())));
		}
		return blocs;
	}

}
