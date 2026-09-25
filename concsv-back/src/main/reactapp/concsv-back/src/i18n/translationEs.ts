const translationEs = {
    comu: {
        empty: {
            option: 'Selecciona...',
        },
    },
    // Claves compartidas por los componentes genéricos (StyledMuiGrid, StyledMuiFilter...). Toda
    // etiqueta de acción (botones, menús, tooltips) va en imperativo: "Guarda", "Cancela"...
    common: {
        save: 'Guarda',
        create: 'Crea',
        update: 'Modifica',
        delete: 'Elimina',
        refresh: 'Refresca',
        cancel: 'Cancela',
        close: 'Cierra',
        clear: 'Limpia',
        filter: 'Filtra',
        processing: 'Procesando...',
        filterCount_one: '{{num}} filtro aplicado',
        filterCount_other: '{{num}} filtros aplicados',
        advancedSearch: 'Búsqueda avanzada',
        advancedSearchOpen: 'Abre la búsqueda avanzada',
        advancedSearchClose: 'Cierra la búsqueda avanzada',
    },
    app: {
        loading: 'Iniciando CONCSV',
        sessio: {
            caducada: 'La sesión ha caducado. Vuelve a iniciar la sesión para continuar.',
            iniciar: 'Inicia la sesión',
            error: 'No se ha podido obtener la sesión del usuario.',
            tornarAProvar: 'Vuelve a intentarlo',
        },
        menu: {
            home: 'Inicio',
            entitats: 'Entidades',
            avisos: 'Avisos',
            documentsExclosos: 'Documentos excluidos',
            propietats: 'Propiedades de sistema',
            integracions: 'Monitor de integraciones',
            cacheDocuments: 'Caché de documentos',
        },
    },
    page: {
        forbidden: {
            message: 'No tiene acceso a esta página con el rol actual',
        },
        notFound: {
            message: 'Página no encontrada',
        },
        entitats: {
            grid: {
                title: 'Entidades',
            },
            form: {
                // El diálogo de la grilla compone el título con el verbo de la acción ("Crea" o
                // "Modifica") más este nombre de recurso.
                resourceTitle: 'entidad',
                seccioAparenca: 'Apariencia de la cabecera',
            },
            accio: {
                nova: 'Nueva entidad',
                modificar: 'Modifica',
                activar: 'Activa',
                desactivar: 'Desactiva',
                esborrar: 'Elimina',
                crearOk: 'La entidad se ha creado correctamente',
                modificarOk: 'La entidad se ha modificado correctamente',
                esborrarOk: 'La entidad se ha borrado correctamente',
                activarOk: 'La entidad se ha activado correctamente',
                desactivarOk: 'La entidad se ha desactivado correctamente',
                error: 'No se ha podido ejecutar la acción',
            },
        },
        enConstruccio: {
            missatge: 'Esta pantalla todavía está en construcción.',
        },
        home: {
            toolbar: {
                title: 'CONCSV',
                subtitle: 'Backoffice de consulta de documentos con código seguro de verificación.',
            },
        },
    },
    component: {
        Offline: {
            message: 'No se ha podido conectar con el servidor',
            retry: 'Reinténtalo',
        },
        BackdropLoading: {
            close: {
                check: '¿Está seguro de que quiere cerrar esta ventana?',
                description: 'La acción continuará en segundo plano y podrá consultar el resultado más tarde.',
            },
            cancel: {
                check: '¿Está seguro de que quiere cancelar la acción?',
                description: 'La acción se detendrá y no se completará.',
            },
        },
        UserProfile: {
            perfil: 'Mi perfil',
            seccioDades: 'Datos',
            seccioConfig: 'Configuración',
            rols: 'Roles',
            tema: {
                label: 'Tema',
                clar: 'Claro',
                obscur: 'Oscuro',
                dracula: 'Dracula',
                sistema: 'Sistema',
            },
            estilMenu: {
                label: 'Estilo del menú',
                tema: 'Tema',
                temaInvertit: 'Tema invertido',
                peu: 'Fijo',
            },
        },
        EntitatRolSelector: {
            rol: {
                CSV_SUPER: 'Superusuario',
                tothom: 'Usuario',
            },
        },
        MassiveActionSelector: {
            options: 'Opciones',
            all: 'Selecciona todo',
            clear: 'Limpia la selección',
        },
    },
};

export default translationEs;
