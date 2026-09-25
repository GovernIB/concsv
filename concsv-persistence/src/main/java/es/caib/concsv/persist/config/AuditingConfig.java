package es.caib.concsv.persist.config;

import es.caib.concsv.persist.base.config.BaseAuditingConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuració per a les entitats de base de dades auditables. L'auditor és el codi de l'usuari
 * autenticat (veure {@link BaseAuditingConfig}).
 *
 * @author Límit Tecnologies
 */
@Configuration
@EnableJpaAuditing
public class AuditingConfig extends BaseAuditingConfig {

}
