-- ---------------------------------------------------------------------------------------------
-- #18 No generar la versió imprimible dels PDF portafoli i permetre la descàrrega directa
-- ---------------------------------------------------------------------------------------------
-- Propietat es.caib.concsv.imprimible.exclusio: PDF dels quals no es genera la còpia autèntica
-- imprimible (només se'n pot descarregar l'original). Valors:
--   DESACTIVAT         cap (comportament anterior a la 2.2.2)
--   PORTAFOLI          només els PDF portafoli (valor inicial)
--   PORTAFOLI_ADJUNTS  els PDF portafoli i els PDF amb fitxers adjunts
-- ---------------------------------------------------------------------------------------------

INSERT INTO CSV_CONFIG_TYPE (CODE, VALUE) VALUES ('IMPRIMIBLE_EXCLUSIO', 'DESACTIVAT,PORTAFOLI,PORTAFOLI_ADJUNTS');

INSERT INTO CSV_CONFIG (KEY, VALUE, DESCRIPTION, GROUP_CODE, TYPE_CODE, POSITION, JBOSS_PROPERTY) VALUES ('es.caib.concsv.imprimible.exclusio', 'PORTAFOLI', 'PDF dels quals no es genera la còpia autèntica imprimible (només se''n pot descarregar l''original)', 'CONSULTA', 'IMPRIMIBLE_EXCLUSIO', 8, 0);

COMMIT;
