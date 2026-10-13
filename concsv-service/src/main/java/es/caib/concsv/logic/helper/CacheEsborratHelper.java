package es.caib.concsv.logic.helper;

import es.caib.concsv.logic.intf.config.PropertyConfig;
import es.caib.concsv.logic.intf.model.CacheCarpeta;
import es.caib.concsv.logic.intf.model.ConfigResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Recompte i esborrat del contingut de la cache de documents des del backoffice.
 * <p>
 * La cache la gestiona {@code CacheHelper}, que és un bean CDI i per tant no es pot injectar als
 * serveis de Spring del backoffice; aquest helper només fa servir el sistema de fitxers i
 * comparteix amb ell la ruta ({@code <es.caib.concsv.fitxers>/cache/<carpeta>}). Les carpetes es
 * mantenen: només s'esborren els fitxers que contenen.
 * <p>
 * No cal bloquejar res: {@code CacheHelper} ja tolera que un fitxer desaparegui mentre es llegeix.
 *
 * @author Límit Tecnologies
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEsborratHelper {

	private static final String CARPETA_CACHE = "cache";

	private final ConfigHelper configHelper;

	/** Nombre de fitxers que hi ha a la carpeta (0 si la carpeta no existeix). */
	public int comptar(CacheCarpeta carpeta) {
		return fitxers(resolCarpeta(carpeta)).size();
	}

	/** Nombre de fitxers de cada carpeta. */
	public ConfigResource.ResumCache resum() {
		return new ConfigResource.ResumCache(
				comptar(CacheCarpeta.INFO),
				comptar(CacheCarpeta.ORIGINAL),
				comptar(CacheCarpeta.IMPRIMIBLE),
				resolArrel().toString());
	}

	/**
	 * Esborra els fitxers de la carpeta. Un fitxer que no es pot esborrar no atura l'esborrat dels
	 * altres: es registra al log i es compta a {@code errors}.
	 */
	public ConfigResource.ResultatEsborratCache esborrar(CacheCarpeta carpeta) {
		int esborrats = 0;
		int errors = 0;
		for (Path fitxer : fitxers(resolCarpeta(carpeta))) {
			try {
				Files.delete(fitxer);
				esborrats++;
			} catch (NoSuchFileException ex) {
				// L'ha esborrat un altre procés (p. ex. un consultor per caducitat) entre el llistat i ara
				log.debug("El fitxer de cache \"{}\" ja no existeix", fitxer);
			} catch (IOException | RuntimeException ex) {
				errors++;
				log.warn("No s'ha pogut esborrar el fitxer de cache \"{}\": {}", fitxer, ex.toString());
			}
		}
		log.info("Cache de documents, carpeta {}: {} fitxers esborrats, {} errors", carpeta, esborrats, errors);
		return new ConfigResource.ResultatEsborratCache(esborrats, errors);
	}

	/** Esborra el contingut de totes les carpetes de la cache. */
	public ConfigResource.ResultatEsborratCache esborrarTotes() {
		int esborrats = 0;
		int errors = 0;
		for (CacheCarpeta carpeta : CacheCarpeta.values()) {
			ConfigResource.ResultatEsborratCache resultat = esborrar(carpeta);
			esborrats += resultat.getEsborrats();
			errors += resultat.getErrors();
		}
		return new ConfigResource.ResultatEsborratCache(esborrats, errors);
	}

	/** Fitxers regulars de la carpeta. No se segueixen enllaços simbòlics. */
	private List<Path> fitxers(Path carpeta) {
		if (!Files.isDirectory(carpeta, LinkOption.NOFOLLOW_LINKS)) {
			return Collections.emptyList();
		}
		try (Stream<Path> paths = Files.walk(carpeta)) {
			return paths.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).collect(Collectors.toList());
		} catch (IOException | UncheckedIOException ex) {
			throw new IllegalStateException("No s'ha pogut llegir la carpeta de cache " + carpeta + ": " + ex.getMessage(), ex);
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
