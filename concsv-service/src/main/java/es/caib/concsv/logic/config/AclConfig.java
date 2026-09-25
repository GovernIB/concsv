package es.caib.concsv.logic.config;

import es.caib.concsv.logic.base.config.BaseAclConfig;
import es.caib.concsv.logic.intf.config.BaseConfig;
import org.springframework.context.annotation.Configuration;

/**
 * Configuració de les ACLs de Spring Security.
 * <p>
 * De moment ConCSV no assigna permisos per ACL (tots els recursos es protegeixen per rol) i les
 * taules {@code csv_acl_*} no existeixen: els beans es creen però no consulten la base de dades
 * fins que algun recurs declari una restricció d'accés de tipus ACL. Quan calgui, s'han de crear
 * les taules i les seqüències {@code CSV_ACL_*_SEQ} amb disparador, com a DISTRIBUCIO.
 *
 * @author Límit Tecnologies
 */
@Configuration
public class AclConfig extends BaseAclConfig {

	@Override
	protected String getDbTablePrefix() {
		return BaseConfig.DB_PREFIX;
	}

	@Override
	protected String getTableSequenceSuffix() {
		return "_seq";
	}

	@Override
	protected boolean isOracleSequenceLegacy() {
		return true;
	}

}
