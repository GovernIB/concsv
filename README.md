<img alt="Logo concsv" height="150" src="https://github.com/GovernIB/concsv/raw/main/assets/logo_concsv.png"/>

# CONCSV 
Consulta de documents per Codi Segur de Verificació Aplicació de frontal per recuperar documents enmagatzemats a l'arxiu i a l'antic sistema de custòdia

- Versió estable: 2.1.2 (branca 2.0)
- Versió Desenvolupament: 2.1.3 (branca dev)


Contextos/entorns
- PRO concsv         https://csv.caib.es/concsvfront/ / https://intranet.caib.es/concsvfront/
- SE concsv          https://se.caib.es/concsvfront/
- PRE concsv         https://proves.caib.es/concsvfront/
- DEV concsv         https://dev.caib.es/concsvfront/

# Entorn de desenvolupament
- Java 11
- Jboss 7.2
- Springboot 2.7.18
- React 18.3.1


# Estructura
CONCSV
- concasv-ear - Mòdul empaquetat d'aplicació
- concsv-api-interna - Mòdul amb l'API REST interna.
- concsv-commons - Mòdul amb classes comunes entre mòduls.
- concsv-ejb - Mòdul de logica de negoci
- concsv-front - Mòdul interficie d'aplicació
- concsv-service - Mòdul amb classes i interfícies per la lògica de serveis.
- concsv-presistence - Mòdul per persistir i consultar les dades des de la BD.

# Rols
Per accedir a CONCSV des de l'interfície web hi pot accedir tothom que sàbiga el CSV corresponent al document a recuperar.

Hi ha un proxy que redirigeix https://vd.caib.es => https://csv.caib.es/concsv

L'aplicació publica un servei REST des de el que se poden recuperar els documents per 2 mètodes: per CSV i per uuid d'Alfresco
i se poden recuperar 3 tipus de documents:

- Original firmat:
/concsvapi/interna/original/{csv}
/concsvapi/interna/original/uuid/{uuid}

- Versió imprimible amb metadades:
/concsvapi/interna/printable/{csv}
/concsvapi/interna/printable/uuid/{uuid}

- Document ENIDOC interoperable:
/concsvapi/interna/enidoc/{csv}
/concsvapi/interna/enidoc/uuid/{uuid}

Per emprar aques servei és necessari el rol: CSV_REST
