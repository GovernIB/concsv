package es.caib.concsv.logic.intf.config;

/**
 * Configuració de les propietats de l'aplicació. Conté constants per les entrades de les diferents propietats.
 *
 * @author Limit Tecnologies
 */
public class PropertyConfig {

	private static final String PROPERTY_PREFIX = BaseConfig.BASE_PACKAGE + ".";

	public static final String PROP_LOGO_PATH = PROPERTY_PREFIX + "logo.path";
	public static final String PROP_QUERY_URL = PROPERTY_PREFIX + "query.url";
	public static final String PROP_PERFORMANCE = PROPERTY_PREFIX + "performance";
	public static final String PROP_LOGS_LOCATION = PROPERTY_PREFIX + "logs.location";
	public static final String PROP_FORCE_VALIDE_CERT = PROPERTY_PREFIX + "forceValideCert";
	public static final String PROP_CONVERT_PDF_TO_IMG = PROPERTY_PREFIX + "convertpdf2img";
	public static final String PROP_AMAGAR_BOTO_ORIGINAL = PROPERTY_PREFIX + "amagar.boto.original";
	public static final String PROP_ARXIU_DOCS_EXCLOSOS_PATH = PROPERTY_PREFIX + "arxiu.documents.exclosos.path";
	public static final String PROP_ESTADISTICAS_DIAS_CONSERVAR = PROPERTY_PREFIX + "estadisticas.dias.conservar";
	public static final String PROP_OPTIONAL_LABEL_METADATA_PATH = PROPERTY_PREFIX + "optionalLabelMetadata.path";
	public static final String PROP_PERSIST_CONTAINER_TRANSACTIONS_DISABLED = PROPERTY_PREFIX + "persist.container-transactions-disabled";
	public static final String PROP_CONSULT_OLD_SAFEKEEPING = PROPERTY_PREFIX + "consult.oldSafeKeeping";
	public static final String PROP_CONSULT_NEW_DIGITAL_ARCHIVE = PROPERTY_PREFIX + "consult.newDigitalArchive";

	public static final String PROP_NEW_DIGITAL_ARCHIVE_ENDPOINT = PROPERTY_PREFIX + "new.digital.archive.endpoint";
	public static final String PROP_NEW_DIGITAL_ARCHIVE_ORG = PROPERTY_PREFIX + "new.digital.archive.organization";
	public static final String PROP_NEW_DIGITAL_ARCHIVE_APP_CLIENT = PROPERTY_PREFIX + "new.digital.archive.app.client";
	public static final String PROP_NEW_DIGITAL_ARCHIVE_USERNAME = PROPERTY_PREFIX + "new.digital.archive.username";
	public static final String PROP_NEW_DIGITAL_ARCHIVE_PASSWORD = PROPERTY_PREFIX + "new.digital.archive.password";
	public static final String PROP_NEW_DIGITAL_ARCHIVE_VERSION = PROPERTY_PREFIX + "new.digital.archive.version";
	public static final String PROP_NEW_DIGITAL_ARCHIVE_TRACES = PROPERTY_PREFIX + "new.digital.archive.traces";

	public static final String PROP_OLD_SAVEKEEPING_ENDPOINT = PROPERTY_PREFIX + "old.savekeeping.endpoint";
	public static final String PROP_OLD_SAVEKEEPING_TIMEOUT = PROPERTY_PREFIX + "old.savekeeping.timeout";

	public static final String PROP_FRONT_API_URL = PROPERTY_PREFIX + "front.api.url";
	public static final String PROP_FRONT_PREVIEW_ENABLED = PROPERTY_PREFIX + "front.preview.enabled";
	public static final String PROP_FRONT_RECAPTCHA_ENABLED = PROPERTY_PREFIX + "front.recaptcha.enabled";
	public static final String PROP_FRONT_RECAPTCHA_SITEKEY = PROPERTY_PREFIX + "front.recaptcha.sitekey";

	public static final String PROP_BASE_PREFIX_VALIDATE_SIGNATURE = PROPERTY_PREFIX;
	public static final String PROP_BASE_PREFIX_DOCUMENT_CONVERTER = PROPERTY_PREFIX;

}
