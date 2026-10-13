-- ConCSV 2.2.1: monitor d'integracions.
--
-- Registre de les peticions que ConCSV fa a les integracions externes (Arxiu, Custòdia Antiga,
-- validació de firmes i conversió de documents).
-- Si l'aplicació es connecta amb un usuari diferent del propietari de l'esquema, descomenteu el
-- GRANT del final (substituïu WWW_CONCSV per l'usuari o rol de l'entorn).

CREATE TABLE CSV_INTEGRACIO_ACCIO
(
  ID                    NUMBER(19)          NOT NULL,
  DATA                  TIMESTAMP(6)        NOT NULL,
  INTEGRACIO_CODI       VARCHAR2(64 CHAR)   NOT NULL,
  DESCRIPCIO            VARCHAR2(1024 CHAR),
  ENDPOINT              VARCHAR2(1024 CHAR),
  PARAMETRES            CLOB,
  TIPUS                 VARCHAR2(32 CHAR),
  TEMPS_RESPOSTA        NUMBER(19),
  ESTAT                 VARCHAR2(16 CHAR)   NOT NULL,
  ENTITAT_CODI          VARCHAR2(64 CHAR),
  ERROR_DESCRIPCIO      VARCHAR2(2000 CHAR),
  EXCEPCIO_MESSAGE      VARCHAR2(2000 CHAR),
  EXCEPCIO_STACKTRACE   CLOB,
  CREATEDBY_CODI        VARCHAR2(64 CHAR)   NOT NULL,
  CREATEDDATE           TIMESTAMP(6)        NOT NULL,
  LASTMODIFIEDBY_CODI   VARCHAR2(64 CHAR),
  LASTMODIFIEDDATE      TIMESTAMP(6)
);

COMMENT ON COLUMN CSV_INTEGRACIO_ACCIO.INTEGRACIO_CODI IS 'ARXIU, CUSTODIA, VALIDACIO_FIRMES o CONVERSIO';
COMMENT ON COLUMN CSV_INTEGRACIO_ACCIO.PARAMETRES IS 'JSON amb els parametres de la peticio (p. ex. hash o uuid)';
COMMENT ON COLUMN CSV_INTEGRACIO_ACCIO.TEMPS_RESPOSTA IS 'Temps de resposta en ms';
COMMENT ON COLUMN CSV_INTEGRACIO_ACCIO.ESTAT IS 'OK o ERROR';

ALTER TABLE CSV_INTEGRACIO_ACCIO ADD (
  CONSTRAINT CSV_INTEGRACIO_ACCIO_PK PRIMARY KEY (ID));

-- El llistat del monitor ordena per data i filtra per integracio i data. Els noms dels índexs
-- no passen dels 30 caràcters per ser compatibles amb versions antigues d'Oracle.
CREATE INDEX CSV_INTEGRACIO_ACCIO_DATA_I ON CSV_INTEGRACIO_ACCIO (DATA);
CREATE INDEX CSV_INTEGRACIO_ACCIO_CODI_I ON CSV_INTEGRACIO_ACCIO (INTEGRACIO_CODI, DATA);

-- GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_INTEGRACIO_ACCIO TO WWW_CONCSV;

-- Es creen buides (VALUE = NULL): mentre no s'hi posi cap valor des del backoffice s'aplica el
-- valor per defecte del codi (monitor actiu i 30 dies de retenció).
-- Requereix les taules de propietats (concsv_2.2.1_05_propietats.sql).

INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.integracio.monitor.actiu', NULL, 'Indica si es registren les peticions a les integracions al monitor (per defecte, sí)', 'GENERAL', 'BOOL', 7, 0);
INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.integracio.monitor.dies.conservar', NULL, 'Dies que es conserven les peticions del monitor d''integracions (per defecte, 30)', 'GENERAL', 'INT', 8, 0);
