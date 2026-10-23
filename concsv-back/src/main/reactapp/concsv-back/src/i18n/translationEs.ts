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
        undo: 'Deshaz',
        create: 'Crea',
        update: 'Modifica',
        delete: 'Elimina',
        refresh: 'Refresca',
        cancel: 'Cancela',
        close: 'Cierra',
        copiat: 'Copiado al portapapeles',
        clear: 'Limpia',
        filter: 'Filtra',
        processing: 'Procesando...',
        filterCount_one: '{{num}} filtro aplicado',
        filterCount_other: '{{num}} filtros aplicados',
        advancedSearch: 'Búsqueda avanzada',
        advancedSearchOpen: 'Avanzado',
        advancedSearchClose: 'Básico',
    },
    app: {
        loading: 'Iniciando CONCSV',
        sessio: {
            caducada: 'La sesión ha caducado. Vuelve a iniciar la sesión para continuar.',
            iniciar: 'Inicia la sesión',
            error: 'No se ha podido obtener la sesión del usuario.',
            tornarAProvar: 'Vuelve a intentarlo',
        },
        sensePermisos: {
            titol: 'Acceso no autorizado',
            missatge: 'El usuario {{usuari}} no tiene permisos para acceder a CONCSV: se requiere el rol CSV_SUPER.',
            tancarSessio: 'Cierra la sesión',
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
        propietats: {
            title: 'Propiedades de sistema',
            empty: 'No hay propiedades',
            accio: {
                sync: {
                    label: 'Sincroniza',
                    confirm:
                        'Se copiará a la base de datos el valor que los ficheros de propiedades del servidor definen para las propiedades editables, y se perderán los valores guardados desde esta pantalla. ¿Quieres continuar?',
                    ok: 'Las propiedades se han sincronizado correctamente',
                    okCount: 'Sincronización hecha: {{count}} propiedades actualizadas',
                    error: 'No se han podido sincronizar las propiedades',
                },
            },
        },
        cacheDocuments: {
            title: 'Caché de documentos',
            descripcio: {
                intro: 'Desde esta pantalla se puede borrar la caché de documentos del servidor. Cada botón borra los ficheros de una de las carpetas de la caché, y el último las borra todas:',
                imprimible: 'PDF imprimible con los márgenes, por UUID o CSV e idioma.',
                info: 'Información del documento recuperada del Archivo y de los firmantes validados con Afirma, para que no haya que volver a consultar el Archivo.',
                original: 'Documento original recuperado del Archivo.',
                final: 'Los documentos que se vuelvan a consultar se obtendrán de nuevo del origen.',
            },
            accio: {
                esborrarCarpeta: 'Borrar {{carpeta}}',
                esborrarTotes: 'Borrar todos los documentos',
                refrescar: 'Refresca el número de documentos',
                confirmCarpeta:
                    'Confirmad que queréis borrar los documentos de la caché que hay en la carpeta {{carpeta}}. Esta acció no se puede deshacer.',
                confirmRecompte: 'Documentos que se borrarán: {{count}}.',
                recompteCarregant: 'Consultando el número de documentos...',
                recompteError: 'No se ha podido consultar el número de documentos.',
                ruta: 'Ruta: {{ruta}}',
                confirmTotes:
                    'Confirmad que queréis borrar la caché de documentos, es decir, todos los documentos de las carpetas IMPRIMIBLE, INFO y ORIGINAL. Las carpetas no se borran, solo su contenido.',
                confirmRecompteTotes:
                    'Documentos que se borrarán: {{count}} (INFO: {{info}}, ORIGINAL: {{original}}, IMPRIMIBLE: {{imprimible}}).',
                ok: 'Ficheros borrados: {{esborrats}}',
                okAmbErrors:
                    'Ficheros borrados: {{esborrats}}. Ficheros que no se han podido borrar: {{errors}}. Consultad el log del servidor.',
                error: 'No se ha podido borrar la caché de documentos',
            },
        },
        avisos: {
            grid: {
                title: 'Gestión de avisos',
            },
            form: {
                resourceTitle: 'aviso',
            },
            accio: {
                nova: 'Nuevo aviso',
                modificar: 'Modifica',
                activar: 'Activa',
                desactivar: 'Desactiva',
                esborrar: 'Elimina',
                crearOk: 'El aviso se ha creado correctamente',
                modificarOk: 'El aviso se ha modificado correctamente',
                esborrarOk: 'El aviso se ha borrado correctamente',
                activarOk: 'El aviso se ha activado correctamente',
                desactivarOk: 'El aviso se ha desactivado correctamente',
                massivaActivarOk: 'Se han activado los avisos seleccionados',
                massivaDesactivarOk: 'Se han desactivado los avisos seleccionados',
                massivaEsborrarOk: 'Se han borrado los avisos seleccionados',
                error: 'No se ha podido ejecutar la acción',
            },
        },
        documentsExclosos: {
            grid: {
                title: 'Fichero de documentos excluidos para la descarga del original',
            },
            form: {
                resourceTitle: 'documento excluido',
            },
            accio: {
                nova: 'Nueva entrada',
                modificar: 'Modifica',
                esborrar: 'Borra',
                descarrega: 'Descarga',
                carrega: 'Carga',
                crearOk: 'La entrada se ha creado correctamente',
                modificarOk: 'La entrada se ha modificado correctamente',
                esborrarOk: 'La entrada se ha borrado correctamente',
                confirmarEsborrat: '¿Seguro que desea borrar {{count}} entradas seleccionadas?',
                massivaEsborrarOk: 'Se han borrado {{count}} entradas',
                importacioOk:
                    'Importación terminada: {{afegits}} añadidas, {{jaExistents}} ya estaban o estaban repetidas y {{descartats}} descartadas por ser demasiado largas',
                error: 'No se ha podido ejecutar la acción',
            },
        },
        integracio: {
            grid: {
                title: 'Monitor de integraciones',
            },
            errorPipelles: 'No se han podido cargar las integraciones',
            pipella: {
                ARXIU: 'Archivo',
                CUSTODIA: 'Custodia antigua',
                VALIDACIO_FIRMES: 'Validación de firmas',
                CONVERSIO: 'Conversión de documentos',
            },
            detail: {
                title: 'Detalle de la petición',
                data: 'Fecha',
                tipus: 'Tipo',
                tipusValor: {
                    ENVIAMENT: 'Envío',
                    RECEPCIO: 'Recepción',
                },
                integracio: 'Integración',
                endpoint: 'Endpoint',
                descripcio: 'Descripción',
                parametres: 'Parámetros',
                tempsResposta: 'Tiempo de respuesta',
                estat: 'Estado',
                errorDescripcio: 'Descripción del error',
                excepcioMessage: 'Mensaje de la excepción',
                excepcioStacktrace: 'Traza de la excepción',
                copyTooltip: 'Copia la traza',
            },
            diagnostic: {
                boto: 'Diagnóstico',
                titol: 'Diagnóstico rápido de las integraciones',
                executant: 'Ejecutando la prueba...',
                executa: 'Volver a ejecutar el diagnóstico',
                executaTotes: 'Volver a ejecutar todo',
                copiaTraca: 'Copiar la traza',
                mostraTraca: 'Mostrar la traza',
                amagaTraca: 'Ocultar la traza',
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
        MassiveActionSelector: {
            options: 'Opciones',
            all: 'Selecciona todo',
            clear: 'Limpia la selección',
        },
        UserLastConection: {
            lastConnection: "Fecha y hora de la última conexión del usuario",
            darreraConnexio: "Última conexión:",
        },
    },
};

export default translationEs;
