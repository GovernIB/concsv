package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.DocumentContent;
import es.caib.concsv.logic.intf.model.DocumentInfo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementació d'una cache basada en sistema de fitxers.
 *
 * <p>Aquesta classe permet emmagatzemar i recuperar dades binàries
 * utilitzant el sistema de fitxers local com a persistència.</p>
 *
 * <p>Característiques principals:</p>
 * <ul>
 *   <li>Cache basada en disc (filesystem)</li>
 *   <li>Separació per tipus de recurs ({@link CacheType})</li>
 *   <li>Expiració automàtica basada en TTL configurat en hores</li>
 *   <li>Sliding expiration (renovació del TTL en cada accés)</li>
 *   <li>Neteja manual de fitxers expirats</li>
 * </ul>
 *
 * <p>El TTL es defineix en hores i es basa en el {@code lastModifiedTime}
 * dels fitxers per determinar si han expirat.</p>
 *
 * @author Limit Tecnologies
 */
@Slf4j
@ApplicationScoped
public class CacheHelper {

	@Inject
	@ConfigProperty(name = PropertyConfig.PROP_FITXERS_PATH)
	private String fitxersPath;
	@Inject
	@ConfigProperty(name = PropertyConfig.PROP_CACHE_TTL_MINUTS, defaultValue = "30")
	private long cacheTtlMinuts;

	/**
	 * Recupera la informació d'un document de la cache.
	 * <p>Mai propaga errors: si l'entrada no es pot llegir (fitxer corrupte, o
	 * {@link java.io.InvalidClassException} perquè la classe {@link DocumentInfo} ha canviat des que
	 * es va desar) s'elimina el fitxer i es retorna {@link Optional#empty()}, de manera que el
	 * consultor torni a obtenir el document de l'origen i el pugui tornar a desar a la cache.
	 * @param id identificador lògic del recurs cachejat (CSV o UUID)
	 * @param isUuid indica si l'identificador és un UUID
	 * @return la informació del document o empty si no existeix / ha expirat / no s'ha pogut llegir
	 */
	public Optional<DocumentInfo> getInfo(String id, boolean isUuid) {
		return get(resolveInfoPath(id, isUuid), DocumentInfo.class);
	}

	public void setInfo(String id, boolean isUuid, DocumentInfo documentInfo) throws IOException {
		set(resolveInfoPath(id, isUuid), documentInfo);
	}

	/**
	 * Recupera el contingut d'un document de la cache.
	 * <p>Comportament:
	 * <ul>
	 *   <li>Retorna {@link Optional#empty()} si el fitxer no existeix</li>
	 *   <li>Elimina i retorna empty si el contingut ha expirat</li>
	 *   <li>Elimina i retorna empty si el contingut no es pot llegir o deserialitzar</li>
	 *   <li>Si és vàlid, renova el TTL (sliding expiration)</li>
	 * </ul>
	 * @param id identificador lògic del recurs cachejat
	 * @param type tipus de cache
	 * @param lang idioma del document
	 * @return el contingut del document o empty si no existeix / ha expirat / no s'ha pogut llegir
	 */
	public Optional<DocumentContent> getContent(String id, CacheType type, String lang) {
		return get(resolveContentPath(id, type, lang), DocumentContent.class);
	}

	/**
	 * Desa o sobrescriu el contingut d'un document a la cache.
	 * <p>Si el fitxer ja existeix, es substitueix completament.
	 * @param id identificador lògic del recurs
	 * @param type tipus de cache
	 * @param lang idioma del document
	 * @param documentContent dades del document a emmagatzemar
	 * @throws IOException si hi ha errors escrivint al sistema de fitxers
	 */
	public void setContent(String id, CacheType type, String lang, DocumentContent documentContent) throws IOException {
		Path file = resolveContentPath(id, type, lang);
		Files.createDirectories(file.getParent());
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
			oos.writeObject(documentContent);
		}
	}

	/**
	 * Elimina tots els fitxers de cache expirats.
	 * <p>Recorr recursivament la carpeta de cache i elimina fitxers
	 * que han superat el TTL configurat.</p>
	 * @return nombre de fitxers eliminats
	 * @throws IOException si hi ha errors accedint al sistema de fitxers
	 */
	public int cleanExpired() throws IOException {
		int removed = 0;
		if (cacheTtlMinuts == 0) {
			return removed;
		}
		Path root = resolveCacheRoot();
		if (!Files.exists(root)) {
			return removed;
		}
		List<Path> files;
		try (Stream<Path> paths = Files.walk(root)) {
			files = paths
				.filter(Files::isRegularFile)
				.collect(Collectors.toList());
		}
		for (Path file: files) {
			long ts = Files.getLastModifiedTime(file).toMillis();
			if (isExpired(ts)) {
				Files.deleteIfExists(file);
				removed++;
			}
		}
		return removed;
	}

	/**
	 * Llegeix una entrada de la cache.
	 * <p>La cache és només una optimització, així que cap error de lectura no s'ha de propagar
	 * al consultor: si l'entrada no es pot recuperar (fitxer corrupte o truncat, error d'accés al
	 * sistema de fitxers, o {@link java.io.InvalidClassException} perquè la classe desada ja no és
	 * compatible amb la classe local després d'un desplegament) s'elimina el fitxer i es retorna
	 * {@link Optional#empty()}, de manera que el document es torni a obtenir de l'origen i es
	 * pugui tornar a desar a la cache en el seu lloc.
	 * @param file fitxer de la cache a llegir
	 * @param type classe esperada de l'objecte cachejat
	 * @return l'objecte cachejat o empty si no existeix / ha expirat / no s'ha pogut llegir
	 */
	private <T extends Serializable> Optional<T> get(Path file, Class<T> type) {
		try {
			if (!Files.exists(file)) {
				return Optional.empty();
			}
			if (isExpired(Files.getLastModifiedTime(file).toMillis())) {
				Files.deleteIfExists(file);
				return Optional.empty();
			}
			Files.setLastModifiedTime(
				file,
				FileTime.from(Instant.now()));
			try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
				// ofNullable perquè a la cache s'hi poden haver desat entrades nul·les
				// (consultes que no han trobat el document)
				return Optional.ofNullable(type.cast(ois.readObject()));
			}
		} catch (Exception ex) {
			log.warn(
				"No s'ha pogut recuperar l'entrada de cache \"{}\": {}. S'elimina el fitxer i es tornarà a consultar a l'origen.",
				file,
				ex.toString());
			deleteQuietly(file);
			return Optional.empty();
		}
	}

	/** Elimina un fitxer de la cache sense propagar els errors. */
	private void deleteQuietly(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (Exception ex) {
			log.warn("No s'ha pogut eliminar el fitxer de cache \"{}\": {}", file, ex.toString());
		}
	}

	private <T extends Serializable> void set(Path file, T object) throws IOException {
		Files.createDirectories(file.getParent());
		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
			oos.writeObject(object);
		}
	}

	private Path resolveInfoPath(String id, boolean isUuid) {
		String safeId = id.replaceAll("[^a-zA-Z0-9._-]", "_");
		return resolveCacheRoot().
			resolve("INFO").
			resolve((isUuid ? "UUID_" : "CSV_") + safeId);
	}

	private Path resolveContentPath(String id, CacheType type, String lang) {
		String safeId = id.replaceAll("[^a-zA-Z0-9._-]", "_");
		String safeIdWithLang = lang != null ? safeId + "_" + lang : safeId;
		return resolveCacheRoot().
			resolve(type.name()).
			resolve(safeIdWithLang);
	}

	private Path resolveCacheRoot() {
		return Paths.get(fitxersPath, "cache");
	}

	private boolean isExpired(long timestamp) {
		if (cacheTtlMinuts == 0) return false;
		return (System.currentTimeMillis() - timestamp) > ttlMillis();
	}

	private long ttlMillis() {
		return Duration.ofMinutes(cacheTtlMinuts).toMillis();
	}

	public enum CacheType {
		ORIGINAL,
		IMPRIMIBLE
	}

	/** Mètodes per fer tests */

	public void setFitxersPath(String fitxersPath) {
		this.fitxersPath = fitxersPath;
	}

	public void setCacheTtlMinuts(long cacheTtlMinuts) {
		this.cacheTtlMinuts = cacheTtlMinuts;
	}

	@Getter
	@AllArgsConstructor
	private static class ExceptionInfo implements Serializable {
		private Class<? extends Throwable> exceptionType;
		private String exceptionMessage;
		private Throwable cause;
	}

}
