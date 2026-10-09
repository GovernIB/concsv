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
        clear: 'Neteja',
        filter: 'Filtra',
        processing: 'Processant...',
        filterCount_one: '{{num}} filtre aplicat',
        filterCount_other: '{{num}} filtres aplicats',
        advancedSearch: 'Cerca avançada',
        advancedSearchOpen: 'Obre la cerca avançada',
        advancedSearchClose: 'Tanca la cerca avançada',
    },
    app: {
        loading: 'Iniciant CONCSV',
        sessio: {
            caducada: 'La sessió ha caducat. Torna a iniciar la sessió per continuar.',
            iniciar: 'Inicia la sessió',
            error: "No s'ha pogut obtenir la sessió de l'usuari.",
            tornarAProvar: 'Torna-ho a provar',
        },
        menu: {
            home: 'Inici',
            entitats: 'Entitats',
            avisos: 'Avisos',
            documentsExclosos: 'Documents exclosos',
            propietats: 'Propietats de sistema',
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
            title: 'Propietats de sistema',
            empty: 'No hi ha propietats',
            accio: {
                sync: {
                    label: 'Sincronitza',
                    confirm:
                        "Es copiarà a la base de dades el valor que els fitxers de propietats del servidor defineixen per a les propietats editables, i es perdran els valors que s'hagin desat des d'aquesta pantalla. Vols continuar?",
                    ok: "Les propietats s'han sincronitzat correctament",
                    okCount: 'Sincronització feta: {{count}} propietats actualitzades',
                    error: "No s'han pogut sincronitzar les propietats",
                },
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
        EntitatRolSelector: {
            rol: {
                CSV_SUPER: 'Superusuari',
                tothom: 'Usuari',
            },
        },
        MassiveActionSelector: {
            options: 'Opcions',
            all: 'Selecciona-ho tot',
            clear: 'Neteja la selecció',
        },
    },
};

export default translationCa;
