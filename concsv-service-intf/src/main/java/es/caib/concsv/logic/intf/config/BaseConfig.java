package es.caib.concsv.logic.intf.config;

/**
 * Clase amb constants per la cofiguració de l'aplicació.
 *
 * @author Límit Tecnologies
 */
public class BaseConfig {

	// Nom i prefix de taules
	public static final String APP_NAME = "concsv";
	public static final String DB_PREFIX = "csv_";

	// Package per l'aplicació
	public static final String BASE_PACKAGE = "es.caib." + APP_NAME;
	public static final String PROPERTY_PREFIX = BASE_PACKAGE + ".";

	// Propietats
	public static final String APP_PROPERTIES = BASE_PACKAGE + ".properties";
	public static final String APP_SYSTEM_PROPERTIES = BASE_PACKAGE + ".system.properties";

	// Rols reconeguts a l'aplicació. ROLE_USER no és cap rol de Keycloak: és el rol base que el
	// backoffice concedeix a qualsevol usuari autenticat (veure WebSecurityConfig).
	public static final String ROLE_SUPER = "CSV_SUPER";
	public static final String ROLE_USER = "tothom";

	// Rutes relatives
	public static final String API_PATH = "/api";
	public static final String PING_PATH = "/ping";
	public static final String SYSENV_PATH = "/sysenv";
	public static final String MANIFEST_PATH = "/manifest";
	public static final String AUTH_TOKEN_PATH = "/authToken";
	public static final String AUTH_ROLES_PATH = "/authRoles";
	public static final String REACT_APP_PATH = "/reactapp";

	public static final String DEFAULT_LOCALE = "ca";

	// Capçaleres HTTP amb què la interfície REACT indica el rol i l'entitat amb què opera
	public static final String PROP_SECURITY_ROLE_HTTP_HEADER = PROPERTY_PREFIX + "security.selected.role.http.header";
	public static final String PROP_USER_SESSION_HTTP_HEADER = PROPERTY_PREFIX + "user.session.http.header";

}
