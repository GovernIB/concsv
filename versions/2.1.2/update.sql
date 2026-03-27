-- Script d'actualització de BD per la versió 2.1.2 en què es crea la BD i taules per guardar dades de Comanda.

-- Create el tablespace
CREATE TABLESPACE CONCSV
 LOGGING
  DATAFILE 'CONCSV.ORA'
SIZE 128M REUSE  AUTOEXTEND  ON NEXT  1024K MAXSIZE  128M
DEFAULT  STORAGE ( INITIAL 40K NEXT 40K MINEXTENTS 1 MAXEXTENTS  2147483645 PCTINCREASE 50 );

CREATE USER concsv identified by concsv default tablespace CONCSV TEMPORARY TABLESPACE temp;
GRANT CONNECT, RESOURCE, CREATE PUBLIC SYNONYM, drop public synonym, UNLIMITED TABLESPACE TO CONCSV;

-- Crea les taules
CREATE TABLE csv_explot_dim (
    id NUMBER PRIMARY KEY,
    tipus VARCHAR2(255 CHAR) NOT NULL,
    origen VARCHAR2(255 CHAR) NOT NULL,
    CONSTRAINT csv_explot_dim_uk UNIQUE (tipus, origen)
);

CREATE TABLE csv_explot_temps (
    id NUMBER PRIMARY KEY,
    data DATE,
    anualitat NUMBER(10),
    mes NUMBER(10),
    trimestre NUMBER(10),
    setmana NUMBER(10),
    dia NUMBER(10),
    dia_setmana VARCHAR2(255 CHAR)
);

CREATE TABLE csv_explot_fet (
    id NUMBER PRIMARY KEY,
    dimensio_id NUMBER NOT NULL,
    temps_id NUMBER NOT NULL,
    tot_correcte NUMBER,
    tot_codi_invalid NUMBER,
    tot_error NUMBER,
    temps_mig_correcte NUMBER,
    CONSTRAINT fk_csv_explot_fet_dim FOREIGN KEY (dimensio_id)
        REFERENCES csv_explot_dim(id),
    CONSTRAINT fk_csv_explot_fet_temps FOREIGN KEY (temps_id)
        REFERENCES csv_explot_temps(id)
);

-- Crea la seqüència pels identificadors
CREATE SEQUENCE CSV_HIBERNATE_SEQ
    START WITH 1
    INCREMENT BY 1
    NOCACHE
    NOCYCLE;

-- Crea els índexos
CREATE INDEX CSV_EXPLOT_FET_DIM_FK_I ON CSV_EXPLOT_FET(DIMENSIO_ID);
CREATE INDEX CSV_EXPLOT_FET_TEMPS_FK_I ON CSV_EXPLOT_FET(TEMPS_ID);

-- Assigna permisos a l'usuari web.
GRANT SELECT, UPDATE, INSERT, DELETE ON csv_explot_dim TO WWW_CONCSV;
GRANT SELECT, UPDATE, INSERT, DELETE ON csv_explot_temps TO WWW_CONCSV;
GRANT SELECT, UPDATE, INSERT, DELETE ON csv_explot_fet TO WWW_CONCSV;
GRANT SELECT ON CSV_HIBERNATE_SEQ TO WWW_CONCSV;
