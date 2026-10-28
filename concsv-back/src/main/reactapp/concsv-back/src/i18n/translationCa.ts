const translationCa = {
    comu: {
        empty: {
            option: 'Selecciona...',
        },
    },
    // Claus compartides pels components genèrics (StyledMuiGrid, StyledMuiFilter...). Tota
    // etiqueta d'acció (botons, menús, tooltips) va en imperatiu: "Desa", "Cancel·la"...
    common: {
        save: 'Desa',
        undo: 'Desfés',
        create: 'Crea',
        update: 'Modifica',
        delete: 'Esborra',
        refresh: 'Refresca',
        cancel: 'Cancel·la',
        close: 'Tanca',
        copiat: 'Copiat al portapapers',
        clear: 'Neteja',
        filter: 'Filtra',
        processing: 'Processant...',
        filterCount_one: '{{num}} filtre aplicat',
        filterCount_other: '{{num}} filtres aplicats',
        advancedSearch: 'Cerca avançada',
        advancedSearchOpen: 'Avançat',
        advancedSearchClose: 'Bàsic',
    },
    app: {
        loading: 'Iniciant CONCSV',
        sessio: {
            caducada: 'La sessió ha caducat. Torna a iniciar la sessió per continuar.',
            iniciar: 'Inicia la sessió',
            error: "No s'ha pogut obtenir la sessió de l'usuari.",
            tornarAProvar: 'Torna-ho a provar',
        },
        sensePermisos: {
            titol: 'Accés no autoritzat',
            missatge: "L'usuari {{usuari}} no té permisos per accedir a CONCSV: cal el rol CSV_SUPER.",
            tancarSessio: 'Tanca la sessió',
        },
        footer: {
            accessibilitat: 'Accessibilitat',
        },
        menu: {
            home: 'Inici',
            entitats: 'Entitats',
            avisos: 'Avisos',
            documentsExclosos: 'Documents exclosos',
            propietats: 'Propietats de configuració',
            integracions: "Monitor d'integracions",
            cacheDocuments: 'Cache de documents',
        },
    },
    page: {
        forbidden: {
            message: 'No teniu accés a aquesta pàgina amb el rol actual',
        },
        notFound: {
            message: 'Pàgina no trobada',
        },
        accesibilitat: {
            title: "Declaració d'Accessibilitat",
            intro: {
                title: "Introducció",
                p1Part1: "El Govern de les Illes Balears s'ha compromès a fer accessible el seu lloc web i la seva aplicació per a dispositius mòbils, de conformitat amb",
                p1LinkText: "el Reial decret 1112/2018",
                p1Part2: ", de 7 de setembre, d'accessibilitat dels llocs web i aplicacions mòbils del sector públic.",
                p2Part1: "La present declaració d'accessibilitat s'aplica al lloc web",
                p2Part2: "i exclou les pàgines que condueixen a enllaços externs.",
            },
            compliment: {
                title: "Situació de compliment",
                introPart1: "Aquest lloc web és parcialment conforme amb",
                introLinkText: "el RD 1112/2018",
                introPart2: "a causa de les excepcions i de la manca de conformitat dels aspectes que s'indiquen a continuació.",
                criteri1: "Criteri A - 4.1.2 Name, Role, Value: Alguns botons iconogràfics mancaven d'alternativa textual accessible. Solució aplicada: s'ha afegit l'atribut \"title\" i l'atribut \"aria-label\" amb text descriptiu a tots els botons que no disposaven d'etiqueta visible, garantint que els lectors de pantalla puguin identificar la seva funció.",
                criteri2: "Criteri A - 1.1.1 Non-text Content: Algunes imatges informatives no disposaven de text alternatiu. Solució aplicada: s'ha realitzat una auditoria de tots els recursos gràfics. Les imatges no decoratives inclouen ara un atribut \"alt\" descriptiu i contextual. Les imatges purament decoratives utilitzen \"alt\" buit o \"role=presentation\" per ser ignorades per les tecnologies de suport.",
                criteri3: "Criteri AA - 1.4.4 Resize Text: El text es truncava en escalar la interfície al 200%. Solució aplicada: en mides de pantalla reduïdes, els botons mostren únicament la icona acompanyada d'un atribut \"title\" descriptiu. S'ha eliminat l'ús de \"overflow: hidden\" en contenidors de text i s'ha verificat que tot el contingut romangui accessible amb zoom del 200%.",
                criteri4: "Criteri AA - 2.5.8 Target Size (Minimum): Determinats elements interactius no complien amb la mida mínima de 24x24 píxels o l'espaiat requerit. Solució aplicada: s'ha modificat el posicionament i maquetació dels components per garantir una àrea de polsació adequada i un espaiat mínim de 8 píxels entre elements interactius, facilitant-ne l'ús en dispositius tàctils i per a persones amb dificultats de mobilitat.",
            },
            noAccesible: {
                title: "Llista de contingut no accessible i explicació del motiu",
                item1: {
                    title: "Formularis i etiquetes",
                    desc1: "No es realitza l'associació explícita adequadament entre controls i etiquetes.",
                    desc2: "Als camps de formulari de tipus selector (filtres de les pantalles d'Entitats i del Monitor d'integracions), l'atribut \"for\" de l'etiqueta no coincideix amb l'identificador de cap camp de formulari, perquè la llibreria Material UI (MUI) assigna l'identificador a l'element que fa de selector i no al camp d'entrada. El validador ho reporta estrictament com a error; el selector disposa igualment d'un nom accessible correcte per als lectors de pantalla mitjançant l'atribut \"aria-labelledby\".",
                },
                item2: {
                    title: "Pantalla de propietats del sistema",
                    desc1: "A la pantalla de configuració de propietats del sistema es detecten un salt en els nivells d'encapçalament, elements de llista fora d'una llista i camps de formulari sense una etiqueta associada programàticament.",
                    desc2: "La descripció de cada propietat és visible al costat del camp, però no hi està vinculada. Està previst corregir-ho en una propera versió.",
                },
                item3: {
                    title: "Múltiples vies de navegació",
                    desc1: "Absència d'un enllaç al mapa web i d'un cercador al lloc.",
                    desc2: "Nivell d'advertència en no proporcionar cap mètode complementari de navegació com un mapa web o una opció de cerca. L'aplicació té un nombre reduït de pantalles, totes accessibles directament des del menú lateral.",
                },
            },
            preparacio: {
                title: "Preparació de la present declaració",
                elaborat: "Aquesta declaració s'ha elaborat mitjançant autoavaluació realitzada per l'equip de desenvolupament utilitzant l'eina automatitzada: Rastrejador Web de l'Observatori d'Accessibilitat Web.",
                darreraRevisio: "Darrera revisió: 23/10/2026",
                properaRevisio: "Propera revisió programada: 23/10/2027",
                norma: "Norma de referència: UNE-EN 301549:2022, nivells A i AA",
                mostra: "Mostra avaluada: 7 pàgines de l'aplicació, amb el rol de superadministrador",
                resultatTitle: "Resultat",
                puntuacioLabel: "Puntuació mitjana del lloc web",
                puntuacioVal: "9.03",
                nivellLabel: "Nivell d'adequació estimat",
                nivellVal: "AA",
                situacioLabel: "Situació de compliment estimada",
                situacioVal: "Parcialment conforme",
                responsive: "El lloc web està dissenyat per a la seva visualització responsive, de manera que es visualitza de forma òptima en dispositius tauleta i mòbils.",
            },
            contacte: {
                title: "Observacions i dades de contacte",
                p1: "El Govern de les Illes Balears pretén continuar millorant i oferir als ciutadans el millor servei possible. Podeu realitzar comunicacions sobre requisits d'accessibilitat (article 10.2.a) del RD 1112/2018, com per exemple:",
                li1: "Informar sobre qualsevol possible incompliment per part d'aquest lloc web",
                li2: "Transmetre altres dificultats d'accés al contingut",
                li3: "Formular qualsevol altra consulta o suggeriment de millora relativa a l'accessibilitat del lloc web",
                p2Part1: "A través del següent formulari de",
                p2LinkText: "contacte",
                p2Part2: "o trucant al telèfon 971177140. Podeu presentar:",
                li4: "Una queixa relativa al compliment dels requisits del RD 1112/2018, o",
                li5: "Una sol·licitud d'informació accessible relativa a:",
                li5a: "Continguts que estan exclosos de l'àmbit d'aplicació del RD 1112/2018 segons el que estableix l'article 3, apartat 4, o",
                li5b: "Continguts que estan exempts del compliment dels requisits d'accessibilitat per imposar una càrrega desproporcionada.",
                p4Part1: "A través del següent procediment:",
                p4LinkText: "Peticions d'informació accessible i queixes relatives a l'accessibilitat de llocs web i aplicacions mòbils.",
            },
            procediment: {
                title: "Procediment d'aplicació",
                p1: "El procediment de reclamació recollit a l'article 13 del RD 1112/2018 va entrar en vigor el 20 de setembre de 2020.",
                p2: "Si un cop realitzada una sol·licitud d'informació accessible o una queixa, aquesta ha estat desestimada, no s'està d'acord amb la decisió adoptada, o la resposta no compleix els requisits contemplats a l'article 12.5, la persona interessada podrà iniciar una reclamació. Igualment, es podrà iniciar una reclamació en el cas que hagi transcorregut el termini de vint dies hàbils sense haver obtingut resposta.",
                p3Part1: "La reclamació pot ser presentada a través del procediment",
                p3LinkText: "Reclamacions relatives a l'accessibilitat de llocs web i aplicacions mòbils",
            },
            opcional: {
                title: "Contingut opcional",
                mesures: "Mesures d'accessibilitat addicionals implementades: declaració de l'idioma principal del document, actualitzat dinàmicament segons l'idioma seleccionat per l'usuari; estructura d'encapçalaments revisada (eliminació d'encapçalaments buits i dels salts de nivell a les pantalles amb graelles); estructura de llista corregida al menú de navegació lateral.",
                config: "Configuració tècnica recomanada: navegadors actualitzats (Chrome, Firefox, Edge, Safari en les seves dues darreres versions), resolució mínima de 1280x720 píxels, i suport de zoom fins al 200% sense pèrdua de contingut o funcionalitat.",
                recursos: "Recursos d'interès: Guia d'accessibilitat web del W3C (https://www.w3.org/WAI/), validador automàtic WAVE (https://wave.webaim.org/), i documentació oficial del Reial Decret 1112/2018 (https://www.boe.es/buscar/act.php?id=BOE-A-2018-12699).",
            },
        },
        entitats: {
            grid: {
                title: 'Entitats',
            },
            form: {
                // El diàleg de la graella compon el títol amb el verb de l'acció ("Crea" o
                // "Modifica") més aquest nom de recurs.
                resourceTitle: 'entitat',
                seccioAparenca: 'Aparença de la capçalera',
            },
            accio: {
                nova: 'Nova entitat',
                modificar: 'Modifica',
                activar: 'Activa',
                desactivar: 'Desactiva',
                esborrar: 'Esborra',
                crearOk: "L'entitat s'ha creat correctament",
                modificarOk: "L'entitat s'ha modificat correctament",
                esborrarOk: "L'entitat s'ha esborrat correctament",
                activarOk: "L'entitat s'ha activat correctament",
                desactivarOk: "L'entitat s'ha desactivat correctament",
                error: "No s'ha pogut executar l'acció",
            },
        },
        propietats: {
            title: 'Propietats de configuració',
            empty: 'No hi ha propietats',
            accio: {
                sync: {
                    label: 'Sincronitza amb JBoss',
                    confirm:
                        "Es copiarà a la base de dades el valor que els fitxers de propietats del servidor defineixen per a les propietats editables, i es perdran els valors que s'hagin desat des d'aquesta pantalla. Vols continuar?",
                    ok: "Les propietats s'han sincronitzat correctament",
                    okCount: 'Sincronització feta: {{count}} propietats actualitzades',
                    error: "No s'han pogut sincronitzar les propietats",
                },
            },
        },
        cacheDocuments: {
            title: 'Cache de documents',
            descripcio: {
                intro: "Des d'aquesta pantalla es pot esborrar la cache de documents del servidor. Cada botó esborra els fitxers d'una de les carpetes de la cache, i el darrer les esborra totes:",
                imprimible: 'PDF imprimible amb els marges, per UUID o CSV i idioma.',
                info: "Informació del document recuperada de l'Arxiu i dels signants validats amb Afirma, perquè no s'hagi de tornar a consultar l'Arxiu.",
                original: "Document original recuperat de l'Arxiu.",
                final: "Els documents que es tornin a consultar s'obtindran de nou de l'origen.",
            },
            accio: {
                esborrarCarpeta: 'Esborra {{carpeta}}',
                esborrarTotes: 'Esborra tots els documents',
                refrescar: 'Refresca el nombre de documents',
                confirmCarpeta:
                    "Confirmau que voleu esborrar els documents de la cache que hi ha a la carpeta {{carpeta}}. Aquesa acció no es pot desfer.",
                confirmRecompte: "Documents que s'esborraran: {{count}}.",
                recompteCarregant: 'Consultant el nombre de documents...',
                recompteError: "No s'ha pogut consultar el nombre de documents.",
                ruta: 'Ruta: {{ruta}}',
                confirmTotes:
                    "Confirmau que voleu esborrar la cache de documents, és a dir, tots els documents de les carpetes IMPRIMIBLE, INFO i ORIGINAL. Les carpetes no s'esborren, només el seu contingut.",
                confirmRecompteTotes:
                    "Documents que s'esborraran: {{count}} (INFO: {{info}}, ORIGINAL: {{original}}, IMPRIMIBLE: {{imprimible}}).",
                ok: 'Fitxers esborrats: {{esborrats}}',
                okAmbErrors:
                    "Fitxers esborrats: {{esborrats}}. Fitxers que no s'han pogut esborrar: {{errors}}. Consultau el log del servidor.",
                error: "No s'ha pogut esborrar la cache de documents",
            },
        },
        avisos: {
            grid: {
                title: "Gestió d'avisos",
            },
            form: {
                resourceTitle: 'avís',
            },
            accio: {
                nova: 'Nou avís',
                modificar: 'Modifica',
                activar: 'Activa',
                desactivar: 'Desactiva',
                esborrar: 'Esborra',
                crearOk: "L'avís s'ha creat correctament",
                modificarOk: "L'avís s'ha modificat correctament",
                esborrarOk: "L'avís s'ha esborrat correctament",
                activarOk: "L'avís s'ha activat correctament",
                desactivarOk: "L'avís s'ha desactivat correctament",
                massivaActivarOk: "S'han activat els avisos seleccionats",
                massivaDesactivarOk: "S'han desactivat els avisos seleccionats",
                massivaEsborrarOk: "S'han esborrat els avisos seleccionats",
                error: "No s'ha pogut executar l'acció",
            },
        },
        documentsExclosos: {
            grid: {
                title: "Fitxer de documents exclosos per la descàrrega d'original",
            },
            form: {
                resourceTitle: 'document exclòs',
            },
            accio: {
                nova: 'Nova entrada',
                modificar: 'Modifica',
                esborrar: 'Esborra',
                descarrega: 'Descarrega',
                carrega: 'Carrega',
                crearOk: "L'entrada s'ha creat correctament",
                modificarOk: "L'entrada s'ha modificat correctament",
                esborrarOk: "L'entrada s'ha esborrat correctament",
                confirmarEsborrat: "Segur que voleu esborrar {{count}} entrades seleccionades?",
                massivaEsborrarOk: "S'han esborrat {{count}} entrades",
                importacioOk:
                    "Importació acabada: {{afegits}} afegides, {{jaExistents}} ja hi eren o estaven repetides i {{descartats}} descartades per ser massa llargues",
                error: "No s'ha pogut executar l'acció",
            },
        },
        integracio: {
            grid: {
                title: "Monitor d'integracions",
            },
            errorPipelles: "No s'han pogut carregar les integracions",
            pipella: {
                ARXIU: 'Arxiu',
                CUSTODIA: 'Custòdia antiga',
                VALIDACIO_FIRMES: 'Validació de firmes',
                CONVERSIO: 'Conversió de documents',
            },
            detail: {
                title: 'Detall de la petició',
                data: 'Data',
                tipus: 'Tipus',
                tipusValor: {
                    ENVIAMENT: 'Enviament',
                    RECEPCIO: 'Recepció',
                },
                integracio: 'Integració',
                endpoint: 'Endpoint',
                descripcio: 'Descripció',
                parametres: 'Paràmetres',
                tempsResposta: 'Temps de resposta',
                estat: 'Estat',
                errorDescripcio: "Descripció de l'error",
                excepcioMessage: "Missatge de l'excepció",
                excepcioStacktrace: "Traça de l'excepció",
                copyTooltip: 'Copia la traça',
            },
            diagnostic: {
                boto: 'Diagnòstic',
                titol: 'Diagnòstic ràpid de les integracions',
                executant: 'Executant la prova...',
                executa: 'Torna a executar el diagnòstic',
                executaTotes: 'Torna a executar tot',
                copiaTraca: 'Copia la traça',
                mostraTraca: 'Mostra la traça',
                amagaTraca: 'Amaga la traça',
            },
        },
        enConstruccio: {
            missatge: 'Aquesta pantalla encara està en construcció.',
        },
        home: {
            toolbar: {
                title: 'CONCSV',
                subtitle: 'Backoffice de consulta de documents amb codi segur de verificació.',
            },
        },
    },
    component: {
        Offline: {
            message: "No s'ha pogut connectar amb el servidor",
            retry: 'Torna-ho a provar',
        },
        BackdropLoading: {
            close: {
                check: 'Estau segur que voleu tancar aquesta finestra?',
                description: "L'acció continuarà en segon pla i podreu consultar el resultat més tard.",
            },
            cancel: {
                check: "Estau segur que voleu cancel·lar l'acció?",
                description: "L'acció s'aturarà i no es completarà.",
            },
        },
        UserProfile: {
            perfil: 'El meu perfil',
            seccioDades: 'Dades',
            seccioConfig: 'Configuració',
            rols: 'Rols',
            tema: {
                label: 'Tema',
                clar: 'Clar',
                obscur: 'Obscur',
                dracula: 'Dracula',
                sistema: 'Sistema',
            },
            estilMenu: {
                label: 'Estil del menú',
                tema: 'Tema',
                temaInvertit: 'Tema invertit',
                peu: 'Fix',
            },
        },
        MassiveActionSelector: {
            options: 'Opcions',
            all: 'Selecciona-ho tot',
            clear: 'Neteja la selecció',
        },
        UserLastConection: {
            lastConnection: "Data i hora de la darrera connexió de l'usuari",
            darreraConnexio: "Darrera connexió:",
        },
    },
};

export default translationCa;
