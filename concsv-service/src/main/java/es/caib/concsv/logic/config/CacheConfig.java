package es.caib.concsv.logic.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuració de les caches de la capa de lògica del backoffice.
 * <p>
 * Es declara el {@link CacheManager} explícitament (en memòria) perquè el context dels EJBs i el
 * de Spring Boot tenguin el mateix, sense dependre de quin proveïdor de cache hi hagi al classpath.
 *
 * @author Límit Tecnologies
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String ACL_CACHE_NAME = "aclCache";

	@Bean
	public CacheManager cacheManager() {
		return new ConcurrentMapCacheManager();
	}

}
