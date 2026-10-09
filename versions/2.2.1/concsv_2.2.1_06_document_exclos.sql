-- ConCSV 2.2.1: manteniment de documents exclosos de la descàrrega de l'original.
--
-- Taula amb els UUID o CSV d'Arxiu dels documents dels quals no es pot descarregar l'original.
-- Substitueix, quan s'adapti el servei, el fitxer de text de la propietat
-- es.caib.concsv.arxiu.documents.exclosos.path.
-- La seqüència CSV_HIBERNATE_SEQ ja existeix des de la 2.0.
-- Si l'aplicació es connecta amb un usuari diferent del propietari de l'esquema, descomenteu el
-- GRANT del final (substituïu WWW_CONCSV per l'usuari o rol de l'entorn).

CREATE TABLE CSV_DOCUMENT_EXCLOS
(
  ID                    NUMBER(19)          NOT NULL,
  VALOR                 VARCHAR2(256 CHAR)  NOT NULL,
  CREATEDBY_CODI        VARCHAR2(64 CHAR)   NOT NULL,
  CREATEDDATE           TIMESTAMP(6)        NOT NULL,
  LASTMODIFIEDBY_CODI   VARCHAR2(64 CHAR),
  LASTMODIFIEDDATE      TIMESTAMP(6)
);

-- Índex únic sobre VALOR: agilita les cerques per aquest camp (el servei consulta si un UUID o CSV 
-- és exclòs a cada descàrrega d'original) i és el que utilitza la restricció d'unicitat.
CREATE UNIQUE INDEX CSV_DOCUMENT_EXCLOS_VALOR_IDX ON CSV_DOCUMENT_EXCLOS (VALOR);

ALTER TABLE CSV_DOCUMENT_EXCLOS ADD (
  CONSTRAINT CSV_DOCUMENT_EXCLOS_PK PRIMARY KEY (ID),
  CONSTRAINT CSV_DOCUMENT_EXCLOS_VALOR_UK UNIQUE (VALOR) USING INDEX CSV_DOCUMENT_EXCLOS_VALOR_IDX);

-- GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_DOCUMENT_EXCLOS TO WWW_CONCSV;
