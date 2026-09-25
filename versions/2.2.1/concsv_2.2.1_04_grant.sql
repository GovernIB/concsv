-- ConCSV 2.2.1: permisos sobre les taules del backoffice.
-- Només cal si l'aplicació es connecta amb un usuari diferent del propietari de l'esquema.
-- Substituïu WWW_CONCSV per l'usuari o rol de l'entorn.

GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_ENTITAT TO WWW_CONCSV;
GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_USUARI TO WWW_CONCSV;
GRANT SELECT, UPDATE, INSERT, DELETE ON CSV_AVIS TO WWW_CONCSV;
