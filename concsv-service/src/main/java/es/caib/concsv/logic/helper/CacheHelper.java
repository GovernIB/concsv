package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.DocumentContent;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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
	@ConfigProperty(name = PropertyConfig.PROP_CACHE_TTL_HORES, defaultValue = "24")
	private long cacheTtlHores;

	/**
	 * Recupera un element de la cache.
	 * <p>Comportament:
	 * <ul>
	 *   <li>Retorna {@link Optional#empty()} si el fitxer no existeix</li>
	 *   <li>Elimina i retorna empty si el contingut ha expirat</li>
	 *   <li>Si és vàlid, renova el TTL (sliding expiration)</li>
	 * </ul>
	 * @param id identificador lògic del recurs cachejat
	 * @param type tipus de cache (separació per domini)
	 * @return les dades del document o empty si no existeix / ha expirat
	 * @throws IOException si hi ha errors d'accés al sistema de fitxers
	 */
	public Optional<DocumentContent> get(String id, CacheType type) throws IOException, ClassNotFoundException {
		Path file = resolvePath(id, type);
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
			DocumentContent documentContent = (DocumentContent)ois.readObject();
			return Optional.of(documentContent);
		}
	}

	/**
	 * Desa o sobrescriu un element a la cache.
	 * <p>Si el fitxer ja existeix, es substitueix completament.
	 * @param id identificador lògic del recurs
	 * @param type tipus de cache
	 * @param documentContent dades del document a emmagatzemar
	 * @throws IOException si hi ha errors escrivint al sistema de fitxers
	 */
	public void set(String id, CacheType type, DocumentContent documentContent) throws IOException {
		Path file = resolvePath(id, type);
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
		if (cacheTtlHores == 0) {
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

	private Path resolvePath(String id, CacheType type) {
		String safeId = id.replaceAll("[^a-zA-Z0-9._-]", "_");
		return resolveCacheRoot().
			resolve(type.name()).
			resolve(safeId);
	}

	private Path resolveCacheRoot() {
		return Paths.get(fitxersPath, "cache");
	}

	private boolean isExpired(long timestamp) {
		if (cacheTtlHores == 0) return false;
		return (System.currentTimeMillis() - timestamp) > ttlMillis();
	}

	private long ttlMillis() {
		return Duration.ofHours(cacheTtlHores).toMillis();
	}

	public enum CacheType {
		ORIGINAL,
		IMPRIMIBLE
	}

}
