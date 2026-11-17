-- ConCSV 2.2.1: dades del document dels documents exclosos (nom, data de captura, CSV/UUID resolts...).
-- Les columnes són opcionals, excepte ESTAT_ACTUALITZACIO: les files existents queden amb l'estat PENDENT.
ALTER TABLE CSV_DOCUMENT_EXCLOS ADD (
  NOM_DOCUMENT             VARCHAR2(512 CHAR),
  DATA_CAPTURA             TIMESTAMP(6),
  CSV                      VARCHAR2(256 CHAR),
  UUID                     VARCHAR2(64 CHAR),
  ORGAN                    VARCHAR2(256 CHAR),
  ORIGEN                   VARCHAR2(64 CHAR),
  UBICACIO                 VARCHAR2(32 CHAR),
  DATA_ACTUALITZACIO       TIMESTAMP(6),
  ESTAT_ACTUALITZACIO      VARCHAR2(16 CHAR) DEFAULT 'PENDENT' NOT NULL,
  MISSATGE_ACTUALITZACIO   VARCHAR2(1000 CHAR)
);

COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.NOM_DOCUMENT IS 'Nom del document, llegit de l''Arxiu Digital o de la custòdia antiga';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.DATA_CAPTURA IS 'Data de captura del document';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.CSV IS 'CSV del document (el del valor, o el resolt a partir de l''UUID)';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.UUID IS 'UUID del document a l''Arxiu Digital (el del valor, o el resolt a partir del CSV)';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.ORGAN IS 'Òrgan del document (metadada ENI eni:organo)';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.ORIGEN IS 'Origen del document (metadada ENI eni:origen): 0 (ciutadà) o 1 (administració)';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.UBICACIO IS 'On s''ha trobat el document: ARXIU o CUSTODIA_ANTIGA';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.DATA_ACTUALITZACIO IS 'Data de la darrera consulta de les dades del document';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.ESTAT_ACTUALITZACIO IS 'Estat de les dades del document: PENDENT (encara no consultat), OK, NO_TROBAT o ERROR';
COMMENT ON COLUMN CSV_DOCUMENT_EXCLOS.MISSATGE_ACTUALITZACIO IS 'Motiu del NO_TROBAT o de l''ERROR de la darrera consulta';
