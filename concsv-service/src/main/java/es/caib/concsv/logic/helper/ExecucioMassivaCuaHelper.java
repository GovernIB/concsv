package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaResource;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cua de les execucions massives: quina s'ha de processar a continuació, qui la té (node i batec) i
 * com es cancel·la. Les operacions de reclamació i de batec són atòmiques a la base de dades, de
 * manera que si hi ha més d'un node només un processa cada execució.
 * <p>
 * Cada operació s'executa en una transacció pròpia (la reclamació i el batec han de quedar
 * confirmats encara que el procés fallés després), i per això aquesta classe és separada del
 * processador: una crida entre beans passa pel proxy i el {@code REQUIRES_NEW} s'aplica.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecucioMassivaCuaHelper {

	/** Cada quant s'actualitza el batec d'una execució en curs. */
	public static final long HEARTBEAT_INTERVAL_MS = 60_000L;
	/** Temps sense batec a partir del qual s'entén que el node que la processava ha caigut. */
	static final long HEARTBEAT_CADUCITAT_MS = 5 * HEARTBEAT_INTERVAL_MS;
	/** Candidates que es proven de reclamar a cada cicle. */
	static final int MAX_CANDIDATES = 5;

	/** Identificador d'aquest node del servidor; es desa a l'execució que reclama. */
	public static final String NODE = calculaNode();

	private final ExecucioMassivaRepository execucioMassivaRepository;
	private final ExecucioMassivaContingutRepository contingutRepository;

	/**
	 * Reclama la primera execució que aquest node pot processar.
	 *
	 * @return el seu identificador, o {@code null} si no n'hi ha cap.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Long reclamarPerProcessar() {
		LocalDateTime ara = LocalDateTime.now();
		LocalDateTime limitHeartbeat = ara.minusNanos(HEARTBEAT_CADUCITAT_MS * 1_000_000L);
		List<Long> candidates = execucioMassivaRepository.findIdsProcessables(
				ara,
				ExecucioMassivaEstatEnum.PENDENT,
				ExecucioMassivaEstatEnum.EN_CURS,
				NODE,
				limitHeartbeat,
				PageRequest.of(0, MAX_CANDIDATES));
		for (Long id : candidates) {
			// Un node que arribi abans pot haver-se endut aquesta: llavors retorna 0 i es prova la següent.
			if (execucioMassivaRepository.reclamar(
					id,
					ara,
					ExecucioMassivaEstatEnum.PENDENT,
					ExecucioMassivaEstatEnum.EN_CURS,
					NODE,
					limitHeartbeat) == 1) {
				log.info("Node {}: reclamada l'execució massiva {}", NODE, id);
				return id;
			}
		}
		return null;
	}

	/** Renova el batec d'una execució en curs en aquest node. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void actualitzarHeartbeat(Long id) {
		execucioMassivaRepository.updateHeartbeat(id, LocalDateTime.now(), NODE, ExecucioMassivaEstatEnum.EN_CURS);
	}

	/** Si s'ha demanat cancel·lar l'execució; es llegeix de la base de dades a cada crida. */
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public boolean isCancelacioSolicitada(Long id) {
		return execucioMassivaRepository.getCancelacioData(id) != null;
	}

	/**
	 * Cancel·la una execució.
	 * <p>
	 * Si cap node no l'ha agafada, es tanca a l'instant (ella i els seus elements pendents). Si ja
	 * és en curs només es demana la cancel·lació: el processador s'atura abans de l'element
	 * següent, que és quan la tanca.
	 *
	 * @param id
	 *            identificador de l'execució.
	 * @param usuari
	 *            usuari que ho demana.
	 * @return {@link ExecucioMassivaResource#RESULTAT_CANCELADA} o {@link ExecucioMassivaResource#RESULTAT_EN_CURS}.
	 * @throws IllegalStateException
	 *             si no existeix, el tipus no es pot cancel·lar o ja ha acabat.
	 */
	@Transactional
	public String cancelar(Long id, String usuari) {
		ExecucioMassivaEntity execucio = execucioMassivaRepository.findById(id).
				orElseThrow(() -> new IllegalStateException("L'execució massiva no existeix"));
		if (!execucio.getTipus().isCancelable()) {
			throw new IllegalStateException("Aquest tipus d'execució massiva no es pot cancel·lar");
		}
		if (execucio.getDataFi() != null) {
			throw new IllegalStateException("L'execució massiva ja ha acabat");
		}
		LocalDateTime ara = LocalDateTime.now();
		if (execucioMassivaRepository.cancelarPendent(
				id,
				ara,
				usuari,
				ExecucioMassivaEstatEnum.PENDENT,
				ExecucioMassivaEstatEnum.CANCELADA) == 1) {
			contingutRepository.cancelarPendents(
					id,
					ara,
					ExecucioMassivaContingutEstatEnum.PENDENT,
					ExecucioMassivaContingutEstatEnum.CANCELAT);
			log.info("Cancel·lada l'execució massiva {} (pendent) per {}", id, usuari);
			// TODO: Aquí s'hauria de cridar la funció per enviar el correu a l'usuari que ha creat l'execució (camp ENVIAR_CORREU) quan
			//  la cancel·la un altre usuari, un cop confirmada la transacció, si es decideix implementar-ho.
			return ExecucioMassivaResource.RESULTAT_CANCELADA;
		}
		if (execucioMassivaRepository.solicitarCancelacio(id, ara, usuari) == 0) {
			// O ja s'havia demanat (llavors no és cap error), o ha acabat just ara.
			boolean acabada = execucioMassivaRepository.findById(id).
					map(e -> e.getDataFi() != null).
					orElse(true);
			if (acabada) {
				throw new IllegalStateException("L'execució massiva ja ha acabat");
			}
		} else {
			log.info("Demanada la cancel·lació de l'execució massiva {} (en curs) per {}", id, usuari);
		}
		return ExecucioMassivaResource.RESULTAT_EN_CURS;
	}

	/** Nom del node: la propietat de JBoss {@code jboss.node.name}, o si falta el nom de la màquina. */
	static String calculaNode() {
		String node = System.getProperty("jboss.node.name");
		if (node == null || node.trim().isEmpty()) {
			try {
				node = InetAddress.getLocalHost().getHostName();
			} catch (Exception ex) {
				node = null;
			}
		}
		if (node == null || node.trim().isEmpty()) {
			node = "desconegut";
		}
		return node.length() > 255 ? node.substring(0, 255) : node;
	}

}
