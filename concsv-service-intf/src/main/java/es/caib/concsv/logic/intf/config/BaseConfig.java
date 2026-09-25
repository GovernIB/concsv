package es.caib.concsv.logic.intf.config;

/**
 * Clase amb constants per la cofiguració de l'aplicació.
 *
 * @author Límit Tecnologies
 */
public class BaseConfig {

	// Nom i prefix de taules
	public static final String APP_NAME = "concsv";
	public static final String DB_PREFIX = "con_";

	// Package per l'aplicació
	public static final String BASE_PACKAGE = "es.caib." + APP_NAME;

	// Propietats
	public static final String APP_PROPERTIES = BASE_PACKAGE + ".properties";
	public static final String APP_SYSTEM_PROPERTIES = BASE_PACKAGE + ".system.properties";

	// Rols reconeguts a l'aplicació
	public static final String ROLE_SUPER = "CSV_SUPER";

	// Rutes relatives
	public static final String API_PATH = "/api";
	public static final String PING_PATH = "/ping";
	public static final String SYSENV_PATH = "/sysenv";
	public static final String MANIFEST_PATH = "/manifest";
	public static final String AUTH_TOKEN_PATH = "/authToken";
	public static final String AUTH_ROLES_PATH = "/authRoles";
	public static final String REACT_APP_PATH = "/reactapp";
	
	public static final String DEFAULT_LOCALE = "ca";

}
