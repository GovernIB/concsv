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
        footer: {
            accessibilitat: 'Accesibilidad',
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
        accesibilitat: {
            title: "Declaración de Accesibilidad",
            intro: {
                title: "Introducción",
                p1Part1: "El Govern de les Illes Balears se ha comprometido a hacer accesible su sitio web y su aplicación para dispositivos móviles, de conformidad con",
                p1LinkText: "el Real Decreto 1112/2018",
                p1Part2: ", de 7 de septiembre, de accesibilidad de los sitios web y aplicaciones móviles del sector público.",
                p2Part1: "La presente declaración de accesibilidad se aplica al sitio web",
                p2Part2: "y excluye las páginas que conducen a enlaces externos.",
            },
            compliment: {
                title: "Situación de cumplimiento",
                introPart1: "Este sitio web es parcialmente conforme con",
                introLinkText: "el RD 1112/2018",
                introPart2: "a causa de las excepciones y de la falta de conformidad de los aspectos que se indican a continuación.",
                criteri1: "Criterio A - 4.1.2 Name, Role, Value: Algunos botones iconográficos carecían de alternativa textual accesible. Solución aplicada: se ha añadido el atributo \"title\" y el atributo \"aria-label\" con texto descriptivo a todos los botones que no disponían de etiqueta visible, garantizando que los lectores de pantalla puedan identificar su función.",
                criteri2: "Criterio A - 1.1.1 Non-text Content: Algunas imágenes informativas no disponían de texto alternativo. Solución aplicada: se ha realizado una auditoría de todos los recursos gráficos. Las imágenes no decorativas incluyen ahora un atributo \"alt\" descriptivo y contextual. Las imágenes puramente decorativas utilizan \"alt\" vacío o \"role=presentation\" para ser ignoradas por las tecnologías de apoyo.",
                criteri3: "Criterio AA - 1.4.4 Resize Text: El texto se truncaba al escalar la interfaz al 200%. Solución aplicada: en tamaños de pantalla reducidos, los botones muestran únicamente el icono acompañado de un atributo \"title\" descriptivo. Se ha eliminado el uso de \"overflow: hidden\" en contenedores de texto y se ha verificado que todo el contenido permanezca accesible con zoom del 200%.",
                criteri4: "Criterio AA - 2.5.8 Target Size (Minimum): Determinados elementos interactivos no cumplían con el tamaño mínimo de 24x24 píxeles o el espaciado requerido. Solución aplicada: se ha modificado el posicionamiento y maquetación de los componentes para garantizar un área de pulsación adecuada y un espaciado mínimo de 8 píxeles entre elementos interactivos, facilitando su uso en dispositivos táctiles y por personas con dificultades de movilidad.",
            },
            noAccesible: {
                title: "Lista de contenido no accesible y explicación del motivo",
                item1: {
                    title: "Formularios y etiquetas",
                    desc1: "No se realiza la asociación explícita adecuadamente entre controles y etiquetas.",
                    desc2: "En los campos de formulario de tipo selector (filtros de las pantallas de Entidades y del Monitor de integraciones), el atributo \"for\" de la etiqueta no coincide con el identificador de ningún campo de formulario, porque la librería Material UI (MUI) asigna el identificador al elemento que hace de selector y no al campo de entrada. El validador lo reporta estrictamente como error; el selector dispone igualmente de un nombre accesible correcto para los lectores de pantalla mediante el atributo \"aria-labelledby\".",
                },
                item2: {
                    title: "Pantalla de propiedades del sistema",
                    desc1: "En la pantalla de configuración de propiedades del sistema se detectan un salto en los niveles de encabezado, elementos de lista fuera de una lista y campos de formulario sin una etiqueta asociada programáticamente.",
                    desc2: "La descripción de cada propiedad es visible junto al campo, pero no está vinculada. Está previsto corregirlo en una próxima versión.",
                },
                item3: {
                    title: "Múltiples vías de navegación",
                    desc1: "Ausencia de un enlace al mapa web y de un buscador en el sitio.",
                    desc2: "Nivel de advertencia al no proporcionar ningún método complementario de navegación como un mapa web o una opción de búsqueda. La aplicación tiene un número reducido de pantallas, todas accesibles directamente desde el menú lateral.",
                },
            },
            preparacio: {
                title: "Preparación de la presente declaración",
                elaborat: "Esta declaración se ha elaborado mediante autoevaluación realizada por el equipo de desarrollo utilizando la herramienta automatizada: Rastreador Web del Observatorio de Accesibilidad Web.",
                darreraRevisio: "Última revisión: 23/10/2026",
                properaRevisio: "Próxima revisión programada: 23/10/2027",
                norma: "Norma de referencia: UNE-EN 301549:2022, niveles A y AA",
                mostra: "Muestra evaluada: 7 páginas de la aplicación, con el rol de superadministrador",
                resultatTitle: "Resultado",
                puntuacioLabel: "Puntuación media del sitio web",
                puntuacioVal: "9.03",
                nivellLabel: "Nivel de adecuación estimado",
                nivellVal: "AA",
                situacioLabel: "Situación de cumplimiento estimada",
                situacioVal: "Parcialmente conforme",
                responsive: "El sitio web está diseñado para su visualización responsive, de manera que se visualiza de forma óptima en dispositivos tableta y móviles.",
            },
            contacte: {
                title: "Observaciones y datos de contacto",
                p1: "El Govern de les Illes Balears pretende continuar mejorando y ofrecer a los ciudadanos el mejor servicio posible. Podéis realizar comunicaciones sobre requisitos de accesibilidad (artículo 10.2.a) del RD 1112/2018, como por ejemplo:",
                li1: "Informar sobre cualquier posible incumplimiento por parte de este sitio web",
                li2: "Transmitir otras dificultades de acceso al contenido",
                li3: "Formular cualquier otra consulta o sugerencia de mejora relativa a la accesibilidad del sitio web",
                p2Part1: "A través del siguiente formulario de",
                p2LinkText: "contacto",
                p2Part2: "o llamando al teléfono 971177140. Puede presentar:",
                li4: "Una queja relativa al cumplimiento de los requisitos del RD 1112/2018, o",
                li5: "Una solicitud de información accesible relativa a:",
                li5a: "Contenidos que están excluidos del ámbito de aplicación del RD 1112/2018 según lo establecido en el artículo 3, apartado 4, o",
                li5b: "Contenidos que están exentos del cumplimiento de los requisitos de accesibilidad por imponer una carga desproporcionada.",
                p4Part1: "A través del siguiente procedimiento:",
                p4LinkText: "Peticiones de información accesible y quejas relativas a la accesibilidad de sitios web y aplicaciones móviles.",
            },
            procediment: {
                title: "Procedimiento de aplicación",
                p1: "El procedimiento de reclamación recogido en el artículo 13 del RD 1112/2018 entró en vigor el 20 de septiembre de 2020.",
                p2: "Si una vez realizada una solicitud de información accesible o una queja, esta ha sido desestimada, no se está de acuerdo con la decisión adoptada, o la respuesta no cumple los requisitos contemplados en el artículo 12.5, la persona interesada podrá iniciar una reclamación. Igualmente, se podrá iniciar una reclamación en el caso de que haya transcurrido el plazo de veinte días hábiles sin haber obtenido respuesta.",
                p3Part1: "La reclamación puede ser presentada a través del procedimiento",
                p3LinkText: "Reclamaciones relativas a la accesibilidad de sitios web y aplicaciones móviles",
            },
            opcional: {
                title: "Contenido opcional",
                mesures: "Medidas de accesibilidad adicionales implementadas: declaración del idioma principal del documento, actualizado dinámicamente según el idioma seleccionado por el usuario; estructura de encabezados revisada (eliminación de encabezados vacíos y de los saltos de nivel en las pantallas con rejillas); estructura de lista corregida en el menú de navegación lateral.",
                config: "Configuración técnica recomendada: navegadores actualizados (Chrome, Firefox, Edge, Safari en sus dos últimas versiones), resolución mínima de 1280x720 píxeles, y soporte de zoom hasta 200% sin pérdida de contenido o funcionalidad.",
                recursos: "Recursos de interés: Guía de accesibilidad web del W3C (https://www.w3.org/WAI/), validador automático WAVE (https://wave.webaim.org/), y documentación oficial del Real Decreto 1112/2018 (https://www.boe.es/buscar/act.php?id=BOE-A-2018-12699).",
            },
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
