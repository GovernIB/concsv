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
