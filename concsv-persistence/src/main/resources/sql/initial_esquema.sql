CREATE TABLE csv_explot_dim (
    id BIGSERIAL PRIMARY KEY,
    tipus VARCHAR(255) NOT NULL,
    origen VARCHAR(255) NOT NULL,
    CONSTRAINT csv_explot_dim_uk UNIQUE (tipus, origen)
);

CREATE TABLE csv_explot_temps (
    id BIGSERIAL PRIMARY KEY,
    data DATE,
    anualitat INT,
    mes INT,
    trimestre INT,
    setmana INT,
    dia INT,
    dia_setmana VARCHAR(255)
);

CREATE TABLE csv_explot_fet (
    id BIGSERIAL PRIMARY KEY,
    dimensio_id BIGINT NOT NULL REFERENCES csv_explot_dim(id),
    temps_id BIGINT NOT NULL REFERENCES csv_explot_temps(id),
    tot_correcte BIGINT,
    tot_codi_invalid BIGINT,
    tot_error BIGINT,
    temps_mig_correcte BIGINT
);