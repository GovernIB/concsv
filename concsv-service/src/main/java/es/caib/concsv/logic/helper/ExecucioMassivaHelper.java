package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.BaseConfig;
import es.caib.concsv.logic.intf.model.DocumentExclosEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaContingutEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaEstatEnum;
import es.caib.concsv.logic.intf.model.ExecucioMassivaTipusEnum;
import es.caib.concsv.logic.resourceservice.DocumentExclosResourceServiceImpl;
import es.caib.concsv.persist.entity.DocumentExclosEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaContingutEntity;
import es.caib.concsv.persist.entity.ExecucioMassivaEntity;
import es.caib.concsv.persist.repository.ExecucioMassivaContingutRepository;
import es.caib.concsv.persist.repository.ExecucioMassivaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Operacions que fa el procés en segon pla sobre les execucions massives: tractar un element, tancar
 * l'execució i avisar l'usuari (la creació és a {@link ExecucioMassivaCreacioHelper}).
 * <p>
 * Equivalent a {@code ExecucioMassivaHelper} de RIPEA, que només té els tipus d'aquesta aplicació. Cada
 * operació que ha de quedar confirmada per si mateixa (un element, el tancament, el correu) és
 * {@code REQUIRES_NEW}; el processador les crida des d'un altre bean perquè s'apliqui.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecucioMassivaHelper {

	private final ExecucioMassivaRepository execucioMassivaRepository;
	private final ExecucioMassivaContingutRepository contingutRepository;
	private final DocumentExclosResourceServiceImpl documentExclosService;

	/** Identificadors dels elements pendents d'una execució, en ordre de tractament. */
	@Transactional(readOnly = true)
	public List<Long> idsPendents(Long execucioId) {
		return contingutRepository.findIdsPerEstat(execucioId, ExecucioMassivaContingutEstatEnum.PENDENT);
	}

	/** Usuari que va crear l'execució: amb el seu nom es processen els elements (veure {@link #autenticacio}). */
	@Transactional(readOnly = true)
	public String creador(Long execucioId) {
		return execucioMassivaRepository.findById(execucioId).
				map(ExecucioMassivaEntity::getCreatedBy).
				orElseThrow(() -> new IllegalStateException("L'execució massiva " + execucioId + " no existeix"));
	}

	/**
	 * Tracta un element, en una transacció pròpia. Si ja no és pendent no fa res: és el que permet
	 * reprendre una execució interrompida sense repetir feina.
	 * <p>
	 * L'auditoria de les dades que es modifiquen s'omple en el commit d'aquesta transacció, que és
	 * a la sortida d'aquest mètode: per això l'usuari creador ({@link #autenticacio}) l'ha de posar qui el
	 * crida i mantenir-lo durant tota l'execució, no pas el mètode mateix.
	 * <p>
	 * Un error del tractament no es propaga: queda com a estat {@code ERROR} de l'element.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void executaContingut(Long contingutId) {
		ExecucioMassivaContingutEntity contingut = contingutRepository.findById(contingutId).
				orElseThrow(() -> new IllegalStateException("L'element " + contingutId + " no existeix"));
		if (contingut.getEstat() != ExecucioMassivaContingutEstatEnum.PENDENT) {
			return;
		}
		ExecucioMassivaEntity execucio = contingut.getExecucioMassiva();
		contingut.setDataInici(LocalDateTime.now());
		String error;
		try {
			error = processa(execucio.getTipus(), contingut);
		} catch (RuntimeException ex) {
			log.error("Error tractant l'element {} de l'execució massiva {}", contingutId, execucio.getId(), ex);
			error = missatge(ex);
		}
		if (error == null) {
			contingut.updateFinalitzat(LocalDateTime.now());
		} else {
			contingut.updateError(LocalDateTime.now(), error);
		}
		contingutRepository.save(contingut);
	}

	/** El tractament propi de cada tipus. @return el motiu de l'error, o {@code null} si ha anat bé. */
	String processa(ExecucioMassivaTipusEnum tipus, ExecucioMassivaContingutEntity contingut) {
		switch (tipus) {
		case ACTUALITZAR_DADES_DOCUMENTS_EXCLOSOS:
			return actualitzaDocumentExclos(contingut.getElementId());
		default:
			return "Tipus d'execució massiva desconegut: " + tipus;
		}
	}

	/**
	 * Torna a consultar les dades d'un document exclòs. Que el document no es trobi no és cap error de
	 * l'execució (queda reflectit a les dades del document); sí ho és que la consulta falli o que ja
	 * no existeixi.
	 */
	private String actualitzaDocumentExclos(Long documentId) {
		DocumentExclosEntity document = documentExclosService.actualitzaDocument(documentId);
		if (document == null) {
			return "El document exclòs ja no existeix";
		}
		if (DocumentExclosEstatEnum.ERROR == document.getEstatActualitzacio()) {
			String motiu = document.getMissatgeActualitzacio();
			return motiu != null && !motiu.trim().isEmpty() ? motiu : "Error en consultar el document";
		}
		return null;
	}

	/** Marca un element com a error, en una transacció pròpia, quan no s'ha pogut tractar ni desar. */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void marcaContingutError(Long contingutId, String error) {
		contingutRepository.findById(contingutId).ifPresent(contingut -> {
			if (contingut.getEstat() == ExecucioMassivaContingutEstatEnum.PENDENT) {
				contingut.updateError(LocalDateTime.now(), error);
				contingutRepository.save(contingut);
			}
		});
	}

	/**
	 * Tanca una execució, sempre, tant si ha acabat com si s'ha cancel·lat o ha fallat: si es quedés
	 * oberta, la cua la tornaria a agafar. Els elements que no s'han tractat queden cancel·lats (si
	 * s'ha cancel·lat) o amb l'error de l'execució.
	 *
	 * @param error
	 *            motiu pel qual el procés ha fallat, o {@code null}.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finalitza(Long execucioId, String error) {
		LocalDateTime ara = LocalDateTime.now();
		boolean cancelada = execucioMassivaRepository.getCancelacioData(execucioId) != null;
		// Aquestes consultes netegen la sessió: l'execució es carrega després.
		if (cancelada) {
			contingutRepository.cancelarPendents(
					execucioId,
					ara,
					ExecucioMassivaContingutEstatEnum.PENDENT,
					ExecucioMassivaContingutEstatEnum.CANCELAT);
		} else if (error != null) {
			contingutRepository.errorPendents(
					execucioId,
					ara,
					error.length() > ExecucioMassivaContingutEntity.ERROR_MAX ? error.substring(0, ExecucioMassivaContingutEntity.ERROR_MAX) : error,
					ExecucioMassivaContingutEstatEnum.PENDENT,
					ExecucioMassivaContingutEstatEnum.ERROR);
		}
		ExecucioMassivaEntity execucio = execucioMassivaRepository.findById(execucioId).
				orElseThrow(() -> new IllegalStateException("L'execució massiva " + execucioId + " no existeix"));
		execucio.setEstat(cancelada ? ExecucioMassivaEstatEnum.CANCELADA : ExecucioMassivaEstatEnum.FINALITZADA);
		execucio.setDataFi(ara);
		execucioMassivaRepository.save(execucio);
		log.info("Tancada l'execució massiva {} ({})", execucioId, execucio.getEstat());
	}

	/** Autenticació d'un usuari amb permís de superusuari: només les accions que ell ja podia fer (també la del procés, amb l'usuari de sistema). */
	public static Authentication autenticacio(String usuari) {
		return new UsernamePasswordAuthenticationToken(
				usuari,
				null,
				Collections.singletonList(new SimpleGrantedAuthority(BaseConfig.ROLE_SUPER)));
	}

	private static String missatge(Throwable ex) {
		Throwable arrel = ex;
		while (arrel.getCause() != null && arrel.getCause() != arrel) {
			arrel = arrel.getCause();
		}
		String text = arrel.getMessage() != null ? arrel.getMessage() : arrel.getClass().getSimpleName();
		return arrel == ex ? text : ex.getClass().getSimpleName() + ": " + text;
	}

}
