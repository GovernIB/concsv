package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.CacheCarpeta;
import es.caib.concsv.logic.intf.model.CacheDocumentResource;
import es.caib.concsv.logic.intf.model.DocumentInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Llistat i esborrat de les entrades de la cache de documents des del backoffice.
 * <p>
 * La cache la gestiona {@code CacheHelper}, que és un bean CDI i per tant no es pot injectar als
 * serveis de Spring del backoffice; aquest helper només fa servir el sistema de fitxers i
 * comparteix amb ell la ruta ({@code <es.caib.concsv.fitxers>/cache/<carpeta>}) i el temps de
 * vida ({@code cache.ttl.minuts}). La majoria de dades surten del fitxer que dona el sistema
 * operatiu (nom, mida i data de darrera modificació): no s'esborra res en llistar.
 * <p>
 * Per obtenir el nom real del document ({@code CacheDocumentResource.nomDocument}) es
 * deserialitza l'objecte de la carpeta {@code INFO} ({@code DocumentInfo}, sense contingut
 * binari). A {@code ORIGINAL} i {@code IMPRIMIBLE} l'objecte serialitzat és un
 * {@code DocumentContent}, que inclou el {@code byte[]} sencer del document: deserialitzar-lo
 * només per obtenir-ne el nom llegiria tot el fitxer a cada consulta, i amb molts documents
 * cachejats (el comentari de {@code CacheHelper.cleanExpired()} ja parla de "milers de
 * fitxers") podria ser massa lent; per això en aquestes dues carpetes no es deserialitza res.
 * <p>
 * No cal bloquejar res: {@code CacheHelper} ja tolera que un fitxer desaparegui mentre es llegeix.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheDocumentHelper {

	private static final String CARPETA_CACHE = "cache";
	/** Mateix valor per defecte que {@code CacheHelper}. */
	static final long TTL_MINUTS_PER_DEFECTE = 30L;
	/** Separador entre la carpeta i el nom del fitxer a l'identificador de l'entrada. */
	public static final char SEPARADOR_ID = ':';

	private static final String PREFIX_UUID = "UUID_";
	private static final String PREFIX_CSV = "CSV_";
	/** Idioma que {@code CacheHelper} afegeix al final del nom dels imprimibles ({@code _CA}). */
	private static final Pattern SUFIX_IDIOMA = Pattern.compile("_([a-zA-Z]{2,3})$");

	private final ConfigHelper configHelper;

	/**
	 * Fitxer de la cache, amb les dades que en dona el sistema operatiu.
	 */
	@Getter
	public static class Entrada {

		private final CacheCarpeta carpeta;
		private final String nomFitxer;
		private final long mida;
		/** Darrera modificació del fitxer, en mil·lisegons: la darrera consulta de l'entrada. */
		private final long darreraConsulta;
		/** Moment d'expiració en mil·lisegons, o {@code null} si no té temps de vida. */
		private final Long expiracio;
		/** Nom real del document (només a INFO, veure la classe); {@code null} si no s'ha llegit. */
		private final String nomDocument;

		Entrada(CacheCarpeta carpeta, String nomFitxer, long mida, long darreraConsulta, long ttlMinuts, String nomDocument) {
			this.carpeta = carpeta;
			this.nomFitxer = nomFitxer;
			this.mida = mida;
			this.darreraConsulta = darreraConsulta;
			this.expiracio = ttlMinuts > 0 ? darreraConsulta + ttlMinuts * 60_000L : null;
			this.nomDocument = nomDocument;
		}

		/** Identificador de l'entrada: {@code <CARPETA>:<nom del fitxer>}. */
		public String getId() {
			return carpeta.name() + SEPARADOR_ID + nomFitxer;
		}

		/** Dades pròpies del recurs, deduïdes del nom del fitxer. */
		public CacheDocumentResource toResource() {
			String identificador = nomFitxer;
			String tipus = null;
			String idioma = null;
			if (carpeta == CacheCarpeta.INFO) {
				if (nomFitxer.startsWith(PREFIX_UUID)) {
					tipus = "UUID";
					identificador = nomFitxer.substring(PREFIX_UUID.length());
				} else if (nomFitxer.startsWith(PREFIX_CSV)) {
					tipus = "CSV";
					identificador = nomFitxer.substring(PREFIX_CSV.length());
				}
			} else if (carpeta == CacheCarpeta.IMPRIMIBLE) {
				Matcher matcher = SUFIX_IDIOMA.matcher(nomFitxer);
				if (matcher.find() && matcher.start() > 0) {
					idioma = matcher.group(1);
					identificador = nomFitxer.substring(0, matcher.start());
				}
			}
			CacheDocumentResource resource = new CacheDocumentResource();
			resource.setId(getId());
			resource.setCarpeta(carpeta);
			resource.setIdentificador(identificador);
			resource.setTipusIdentificador(tipus);
			resource.setIdioma(idioma);
			resource.setNomFitxer(nomFitxer);
			resource.setNomDocument(nomDocument);
			resource.setMida(mida);
			resource.setDarreraConsulta(new Date(darreraConsulta));
			resource.setExpiracio(expiracio != null ? new Date(expiracio) : null);
			return resource;
		}

	}

	/**
	 * Entrades de totes les carpetes de la cache (cap si no existeixen). El temps de vida es llegeix
	 * un sol cop, no per fitxer.
	 */
	public List<Entrada> llistar() {
		long ttlMinuts = ttlMinuts();
		List<Entrada> entrades = new ArrayList<>();
		for (CacheCarpeta carpeta : CacheCarpeta.values()) {
			entrades.addAll(llistar(carpeta, ttlMinuts));
		}
		return entrades;
	}

	/** Entrada amb l'identificador indicat, si existeix. Un identificador mal format no en troba cap. */
	public Optional<Entrada> trobar(String id) {
		Path fitxer;
		CacheCarpeta carpeta;
		try {
			carpeta = carpetaDeId(id);
			fitxer = resolFitxer(id);
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
		try {
			BasicFileAttributes atributs = Files.readAttributes(fitxer, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			if (!atributs.isRegularFile()) {
				return Optional.empty();
			}
			return Optional.of(entrada(carpeta, fitxer, atributs, ttlMinuts()));
		} catch (NoSuchFileException ex) {
			return Optional.empty();
		} catch (IOException ex) {
			throw new IllegalStateException("No s'ha pogut llegir l'entrada de cache " + fitxer + ": " + ex.getMessage(), ex);
		}
	}

	/**
	 * Esborra les entrades amb els identificadors indicats. Una entrada que no es pot esborrar (o amb
	 * un identificador no vàlid) no atura l'esborrat de les altres: es registra al log i es compta a
	 * {@code errors}. Una entrada que ja no existeix (l'ha esborrada un consultor per caducitat) no és
	 * cap error ni es compta com a esborrada.
	 */
	public CacheDocumentResource.ResultatEsborrat esborrar(List<String> ids) {
		int esborrats = 0;
		int errors = 0;
		if (ids != null) {
			for (String id : new LinkedHashSet<>(ids)) {
				try {
					if (Files.deleteIfExists(resolFitxer(id))) {
						esborrats++;
					} else {
						log.debug("L'entrada de cache \"{}\" ja no existeix", id);
					}
				} catch (IOException | RuntimeException ex) {
					errors++;
					log.warn("No s'ha pogut esborrar l'entrada de cache \"{}\": {}", id, ex.toString());
				}
			}
		}
		log.info("Cache de documents: {} fitxers esborrats, {} errors", esborrats, errors);
		return new CacheDocumentResource.ResultatEsborrat(esborrats, errors);
	}

	/** Entrades d'una carpeta: només fitxers regulars, sense seguir enllaços simbòlics. */
	private List<Entrada> llistar(CacheCarpeta carpeta, long ttlMinuts) {
		Path dir = resolCarpeta(carpeta);
		if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
			return Collections.emptyList();
		}
		try (Stream<Path> paths = Files.list(dir)) {
			return paths.map(p -> entrada(carpeta, p, ttlMinuts)).
					filter(Objects::nonNull).
					collect(Collectors.toList());
		} catch (IOException | UncheckedIOException ex) {
			throw new IllegalStateException("No s'ha pogut llegir la carpeta de cache " + dir + ": " + ex.getMessage(), ex);
		}
	}

	/** Entrada del fitxer, o {@code null} si no és un fitxer regular o ha desaparegut mentre es llistava. */
	private Entrada entrada(CacheCarpeta carpeta, Path fitxer, long ttlMinuts) {
		try {
			BasicFileAttributes atributs = Files.readAttributes(fitxer, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			return atributs.isRegularFile() ? entrada(carpeta, fitxer, atributs, ttlMinuts) : null;
		} catch (IOException ex) {
			log.debug("No s'ha pogut llegir el fitxer de cache \"{}\": {}", fitxer, ex.toString());
			return null;
		}
	}

	private Entrada entrada(CacheCarpeta carpeta, Path fitxer, BasicFileAttributes atributs, long ttlMinuts) {
		return new Entrada(
				carpeta,
				fitxer.getFileName().toString(),
				atributs.size(),
				atributs.lastModifiedTime().toMillis(),
				ttlMinuts,
				carpeta == CacheCarpeta.INFO ? llegirNomDocument(fitxer) : null);
	}

	/**
	 * Nom real del document, llegit de l'objecte {@code DocumentInfo} serialitzat (veure la classe:
	 * només es crida per a la carpeta INFO, que no inclou el contingut binari del document).
	 * <p>
	 * Un error de lectura (fitxer corrupte, no serialitzat, o {@link java.io.InvalidClassException}
	 * perquè la classe ha canviat des que es va desar) no es propaga: es registra i es retorna
	 * {@code null}, com fa {@code CacheHelper.get()}, però SENSE esborrar el fitxer, ja que aquí
	 * només és per mostrar dades i el fitxer pot ser perfectament vàlid per a qui el consulti.
	 */
	private String llegirNomDocument(Path fitxer) {
		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(fitxer))) {
			Object objecte = ois.readObject();
			return objecte instanceof DocumentInfo ? ((DocumentInfo) objecte).getDocumentName() : null;
		} catch (Exception ex) {
			log.debug("No s'ha pogut llegir el nom del document de l'entrada de cache \"{}\": {}", fitxer, ex.toString());
			return null;
		}
	}

	/**
	 * Camí del fitxer d'una entrada. Només accepta una carpeta de {@link CacheCarpeta}, i comprova que
	 * el camí resultant queda directament dins seu (ni en una subcarpeta ni fora, per molt que el nom
	 * contengui {@code ..}, una ruta absoluta o un separador de carpetes): mai no es fia d'un camí
	 * rebut. No es restringeix el joc de caràcters del nom perquè el llistat ja mostra qualsevol
	 * fitxer que hi hagi al disc, encara que el seu nom no sigui el que {@code CacheHelper} hi hauria
	 * escrit (p. ex. copiat a mà per fer proves), i l'esborrat n'ha de poder fer net igualment.
	 *
	 * @throws IllegalArgumentException si l'identificador no és vàlid.
	 */
	Path resolFitxer(String id) {
		CacheCarpeta carpeta = carpetaDeId(id);
		String nom = id.substring(id.indexOf(SEPARADOR_ID) + 1);
		if (nom.isEmpty()) {
			throw new IllegalArgumentException("Nom de fitxer de cache no vàlid: " + nom);
		}
		Path dir = resolCarpeta(carpeta);
		Path fitxer = dir.resolve(nom).normalize();
		if (!dir.equals(fitxer.getParent())) {
			throw new IllegalArgumentException("El fitxer de cache " + fitxer + " és fora de " + dir);
		}
		return fitxer;
	}

	private CacheCarpeta carpetaDeId(String id) {
		int separador = id != null ? id.indexOf(SEPARADOR_ID) : -1;
		if (separador < 1) {
			throw new IllegalArgumentException("Identificador d'entrada de cache no vàlid: " + id);
		}
		try {
			return CacheCarpeta.valueOf(id.substring(0, separador));
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Carpeta de cache no vàlida a l'identificador: " + id);
		}
	}

	/** Minuts de vida d'una entrada ({@code cache.ttl.minuts}, 30 per defecte); 0 vol dir que no expira. */
	long ttlMinuts() {
		String valor = configHelper.getConfig(PropertyConfig.PROP_CACHE_TTL_MINUTS);
		if (valor == null || valor.trim().isEmpty()) {
			return TTL_MINUTS_PER_DEFECTE;
		}
		try {
			return Math.max(0L, Long.parseLong(valor.trim()));
		} catch (NumberFormatException ex) {
			log.warn("Valor no numèric a la propietat {}: \"{}\"", PropertyConfig.PROP_CACHE_TTL_MINUTS, valor);
			return TTL_MINUTS_PER_DEFECTE;
		}
	}

	/** Carpeta arrel de la cache: {@code <es.caib.concsv.fitxers>/cache}. */
	private Path resolArrel() {
		String fitxers = configHelper.getConfig(PropertyConfig.PROP_FITXERS_PATH);
		if (fitxers == null || fitxers.trim().isEmpty()) {
			throw new IllegalStateException("Falta la propietat " + PropertyConfig.PROP_FITXERS_PATH);
		}
		return Paths.get(fitxers, CARPETA_CACHE).toAbsolutePath().normalize();
	}

	private Path resolCarpeta(CacheCarpeta carpeta) {
		Path arrel = resolArrel();
		Path resolta = arrel.resolve(carpeta.name()).normalize();
		if (!resolta.startsWith(arrel)) {
			throw new IllegalStateException("La carpeta de cache " + resolta + " és fora de " + arrel);
		}
		return resolta;
	}

}
