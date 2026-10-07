-- ConCSV 2.2.1: manteniment de propietats configurables del backoffice (grups, tipus i propietats).
--
-- Propietats amb JBOSS_PROPERTY = 1: el seu valor es defineix als fitxers de propietats del servidor
-- (es.caib.concsv.properties i es.caib.concsv.system.properties) i el backoffice les mostra
-- deshabilitades, sense poder-les modificar.
-- Propietats amb JBOSS_PROPERTY = 0: es poden modificar des del backoffice. Els serveis llegeixen
-- el valor de la columna VALUE a cada ús i, si és NULL, el del fitxer de propietats del servidor.
-- Les files es creen amb VALUE = NULL: el valor de cada entorn s'hi copia amb el botó "Sincronitza"
-- de la pantalla "Propietats de sistema" (o es desa a mà); mentrestant mana el fitxer del servidor.
-- Les contrasenyes (tipus CREDENTIALS) no es carreguen mai a la BD ni es poden desar des del backoffice.
-- Per tornar d'emergència als fitxers del servidor, sense reiniciar:
--   UPDATE CSV_CONFIG SET VALUE = NULL WHERE JBOSS_PROPERTY = 0;
-- Després d'executar l'script i desplegar l'aplicació, prémer "Sincronitza" si es vol que la BD dugui
-- els valors dels fitxers del servidor.
-- Els textos duen accents: executeu l'script en UTF-8 (per exemple NLS_LANG=.AL32UTF8).
-- Si l'aplicació es connecta amb un usuari diferent del propietari de l'esquema, descomenteu els
-- GRANT del final (substituïu WWW_CONCSV per l'usuari o rol de l'entorn).

-- ---------------------------------------------------------------------------------------------
-- Taules
-- ---------------------------------------------------------------------------------------------

CREATE TABLE CSV_CONFIG_TYPE
(
  CODE                  VARCHAR2(128 CHAR)  NOT NULL,
  VALUE                 VARCHAR2(2048 CHAR)
);

CREATE TABLE CSV_CONFIG_GROUP
(
  CODE                  VARCHAR2(128 CHAR)  NOT NULL,
  PARENT_CODE           VARCHAR2(128 CHAR),
  POSITION              NUMBER(10)          DEFAULT 0 NOT NULL,
  DESCRIPTION           VARCHAR2(512 CHAR)
);

CREATE TABLE CSV_CONFIG
(
  KEY                   VARCHAR2(256 CHAR)  NOT NULL,
  VALUE                 VARCHAR2(2048 CHAR),
  DESCRIPTION           VARCHAR2(2048 CHAR),
  GROUP_CODE            VARCHAR2(128 CHAR)  NOT NULL,
  TYPE_CODE             VARCHAR2(128 CHAR)  NOT NULL,
  POSITION              NUMBER(10)          DEFAULT 0 NOT NULL,
  JBOSS_PROPERTY        NUMBER(1)           DEFAULT 0 NOT NULL,
  LASTMODIFIEDBY_CODI   VARCHAR2(64 CHAR),
  LASTMODIFIEDDATE      TIMESTAMP(6)
);

-- ---------------------------------------------------------------------------------------------
-- Claus primàries, foranes i índexs
-- ---------------------------------------------------------------------------------------------

ALTER TABLE CSV_CONFIG_TYPE ADD (
  CONSTRAINT CSV_CONFIG_TYPE_PK PRIMARY KEY (CODE));

ALTER TABLE CSV_CONFIG_GROUP ADD (
  CONSTRAINT CSV_CONFIG_GROUP_PK PRIMARY KEY (CODE));

ALTER TABLE CSV_CONFIG ADD (
  CONSTRAINT CSV_CONFIG_PK PRIMARY KEY (KEY));

ALTER TABLE CSV_CONFIG_GROUP ADD CONSTRAINT CSV_CONFIG_GROUP_PARENT_FK
  FOREIGN KEY (PARENT_CODE) REFERENCES CSV_CONFIG_GROUP (CODE);

ALTER TABLE CSV_CONFIG ADD CONSTRAINT CSV_CONFIG_GROUP_FK
  FOREIGN KEY (GROUP_CODE) REFERENCES CSV_CONFIG_GROUP (CODE);

ALTER TABLE CSV_CONFIG ADD CONSTRAINT CSV_CONFIG_TYPE_FK
  FOREIGN KEY (TYPE_CODE) REFERENCES CSV_CONFIG_TYPE (CODE);

CREATE INDEX CSV_CONFIG_GROUP_PARENT_FK_I ON CSV_CONFIG_GROUP (PARENT_CODE);
CREATE INDEX CSV_CONFIG_GROUP_FK_I ON CSV_CONFIG (GROUP_CODE);
CREATE INDEX CSV_CONFIG_TYPE_FK_I ON CSV_CONFIG (TYPE_CODE);

-- ---------------------------------------------------------------------------------------------
-- Dades: tipus de propietat
-- ---------------------------------------------------------------------------------------------

INSERT INTO CSV_CONFIG_TYPE (CODE, VALUE) VALUES ('BOOL', NULL);
INSERT INTO CSV_CONFIG_TYPE (CODE, VALUE) VALUES ('TEXT', NULL);
INSERT INTO CSV_CONFIG_TYPE (CODE, VALUE) VALUES ('INT', NULL);
INSERT INTO CSV_CONFIG_TYPE (CODE, VALUE) VALUES ('CREDENTIALS', NULL);

-- ---------------------------------------------------------------------------------------------
-- Dades: grups de propietats
-- ---------------------------------------------------------------------------------------------

INSERT INTO CSV_CONFIG_GROUP (CODE, PARENT_CODE, POSITION, DESCRIPTION) VALUES ('GENERAL', NULL, 0, 'Configuració general');
INSERT INTO CSV_CONFIG_GROUP (CODE, PARENT_CODE, POSITION, DESCRIPTION) VALUES ('CONSULTA', NULL, 1, 'Consulta de documents');
INSERT INTO CSV_CONFIG_GROUP (CODE, PARENT_CODE, POSITION, DESCRIPTION) VALUES ('CACHE', NULL, 2, 'Memòria cau de documents');
INSERT INTO CSV_CONFIG_GROUP (CODE, PARENT_CODE, POSITION, DESCRIPTION) VALUES ('ARXIU_NOU', NULL, 3, 'Arxiu digital CAIB');
INSERT INTO CSV_CONFIG_GROUP (CODE, PARENT_CODE, POSITION, DESCRIPTION) VALUES ('ARXIU_ANTIC', NULL, 4, 'Sistema de Custòdia Antiga (safekeeping)');
INSERT INTO CSV_CONFIG_GROUP (CODE, PARENT_CODE, POSITION, DESCRIPTION) VALUES ('VALID_SIGN', NULL, 5, 'Plugin de validació de firmes (@firma)');
INSERT INTO CSV_CONFIG_GROUP (CODE, PARENT_CODE, POSITION, DESCRIPTION) VALUES ('CONVERSIO', NULL, 6, 'Plugin de conversió de documents (OpenOffice)');

-- ---------------------------------------------------------------------------------------------
-- Dades: propietats
-- ---------------------------------------------------------------------------------------------

-- Configuració general
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.fitxers', NULL, 'Ruta de la carpeta de fitxers de l''aplicació al servidor', 'GENERAL', 'TEXT', 0, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.logo.path', NULL, 'Ruta del logo que s''incrusta als documents generats', 'GENERAL', 'TEXT', 1, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.logs.location', NULL, 'Carpeta dels logs del servidor', 'GENERAL', 'TEXT', 2, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.performance', NULL, 'Indica si es registren els temps d''execució dels serveis', 'GENERAL', 'BOOL', 3, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.query.url', 'http://vd.caib.es/', 'URL base per consultar un document a partir del seu CSV', 'GENERAL', 'TEXT', 4, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.optionalLabelMetadata.path', '/opt/webapps/concsv', 'Ruta del fitxer d''etiquetes de les metadades opcionals', 'GENERAL', 'TEXT', 5, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.estadisticas.dias.conservar', NULL, 'Dies que es conserven les estadístiques de consulta', 'GENERAL', 'INT', 6, 0);

-- Consulta de documents
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.consult.oldSafeKeeping', NULL, 'Consulta a l''antic sistema de custòdia (safekeeping)', 'CONSULTA', 'BOOL', 0, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.consult.newDigitalArchive', NULL, 'Consulta al nou arxiu digital', 'CONSULTA', 'BOOL', 1, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.forceValideCert', NULL, 'Força la validació dels certificats de les firmes', 'CONSULTA', 'BOOL', 2, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.amagar.boto.original', NULL, 'Amaga per defecte el botó de descàrrega de l''original en documents amb versió imprimible', 'CONSULTA', 'BOOL', 3, 0);

-- Memòria cau
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.cache.activa', NULL, 'Indica si la memòria cau de documents està activa', 'CACHE', 'BOOL', 0, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.cache.ttl.minuts', NULL, 'Minuts que es conserva un document a la memòria cau', 'CACHE', 'INT', 1, 0);

-- Nou arxiu digital
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.new.digital.archive.endpoint', 'https://esbse.caib.es:4430/esb', 'URL del servei', 'ARXIU_NOU', 'TEXT', 0, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.new.digital.archive.organization', 'CAIB', 'Organització', 'ARXIU_NOU', 'TEXT', 1, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.new.digital.archive.app.client', 'TEST', 'Aplicació client', 'ARXIU_NOU', 'TEXT', 2, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.new.digital.archive.username', 'concsv', 'Usuari', 'ARXIU_NOU', 'TEXT', 3, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.new.digital.archive.password', NULL, 'Contrasenya', 'ARXIU_NOU', 'CREDENTIALS', 4, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.new.digital.archive.version', '1.0', 'Versió del servei', 'ARXIU_NOU', 'TEXT', 5, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.new.digital.archive.traces', 'false', 'Indica si es registren les traces de les crides al servei', 'ARXIU_NOU', 'BOOL', 6, 1);

-- Antic sistema de custòdia
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.old.savekeeping.endpoint', 'https://proves.caib.es/signatura/services/ConsultaAnonimaDocumentos', 'URL del servei', 'ARXIU_ANTIC', 'TEXT', 0, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.old.savekeeping.timeout', NULL, 'Temps màxim d''espera de les crides al servei (ms)', 'ARXIU_ANTIC', 'INT', 1, 0);

-- Validació de firmes
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.endpoint', 'https://afirmades2.caib.es/afirmaws/services/DSSAfirmaVerify', 'URL del servei', 'VALID_SIGN', 'TEXT', 0, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.applicationID', 'CAIBDEV2.CONCSV', 'Identificador de l''aplicació', 'VALID_SIGN', 'TEXT', 1, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.authorization.method', 'UsernameToken', 'Mètode d''autenticació', 'VALID_SIGN', 'TEXT', 2, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.authorization.username', 'CONCSV', 'Usuari', 'VALID_SIGN', 'TEXT', 3, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.authorization.password', NULL, 'Contrasenya', 'VALID_SIGN', 'CREDENTIALS', 4, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.TransformersTemplatesPath', '/opt/webapps/afirma/transformers', 'Ruta de les plantilles de transformació', 'VALID_SIGN', 'TEXT', 5, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.printxml', NULL, 'Indica si es mostra el XML de les peticions', 'VALID_SIGN', 'BOOL', 6, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.validatesignature.afirmacxf.debug', NULL, 'Indica si s''activa el mode de depuració', 'VALID_SIGN', 'BOOL', 7, 0);

-- Conversió de documents
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.documentconverter.openoffice.host', '10.35.3.87', 'Servidor d''OpenOffice', 'CONVERSIO', 'TEXT', 0, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.plugins.documentconverter.openoffice.port', '8100', 'Port d''OpenOffice', 'CONVERSIO', 'INT', 1, 1);

-- Aplicació pública de consulta (dins el grup Consulta de documents)
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.front.api.url', 'http://10.35.3.21:8080/concsvfront/api', 'URL de l''API a què accedeix l''aplicació pública', 'CONSULTA', 'TEXT', 4, 1);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.front.preview.enabled', NULL, 'Indica si es mostra la previsualització del document', 'CONSULTA', 'BOOL', 5, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.front.recaptcha.enabled', NULL, 'Indica si la consulta està protegida amb reCAPTCHA', 'CONSULTA', 'BOOL', 6, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.front.recaptcha.sitekey', NULL, 'Clau pública (site key) de reCAPTCHA', 'CONSULTA', 'TEXT', 7, 0);

COMMIT;

-- ---------------------------------------------------------------------------------------------
-- Permisos: només si l'aplicació es connecta amb un usuari diferent del propietari de l'esquema
-- ---------------------------------------------------------------------------------------------
-- GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_CONFIG TO WWW_CONCSV;
-- GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_CONFIG_GROUP TO WWW_CONCSV;
-- GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_CONFIG_TYPE TO WWW_CONCSV;
