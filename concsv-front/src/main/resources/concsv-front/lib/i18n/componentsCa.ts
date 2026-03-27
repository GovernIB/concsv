const componentsCa = {
    baseapp: {
        menu: {
            home: 'Inici',
        },
        auth: {
            logout: 'Tancar sessió',
        },
        error: {
            '404': 'No trobat',
            '503': 'Sense connexió',
        },
        offline: {
            message: 'Sense connexió amb el servidor',
        }
    },
    grid: {
        export: {
            title: 'Exportar',
        },
        create: {
            title: 'Crear',
        },
        update: {
            title: 'Modificar',
        },
        delete: {
            single: {
                title: 'Esborrar',
                confirm: 'Estau segur que voleu esborrar aquest element (aquesta acció no es pot desfer)?',
                success: 'Element esborrat',
            },
            multiple: {
                title: 'Esborrar múltiples elements',
                confirm: 'Estau segur que voleu esborrar aquests {{count}} elements (aquesta acció no es pot desfer)?',
                success: '{{count}} elements esborrats',
                errors: '{{successCount}} elements esborrats de {{totalCount}}',
            },
        },
        refresh: {
            title: 'Refrescar',
        },
        details: {
            title: 'Detalls',
        },
        selection: {
            one: '1 fila seleccionada',
            multiple: '{{count}} files seleccionades',
        },
        error: {
            toolbar: 'S\'ha produit un error',
            validation: 'S\'han produït errors de validació',
        },
        edit: {
            create: 'Crear',
            update: 'Modificar',
            save: 'Desar',
            cancel: 'Cancel·lar'
        },
        tree: {
            header: {
                name: 'Grup',
                expandAll: 'Expandir',
                expandNone: 'Contreure',
            }
        },
    },
    form: {
        goBack: {
            title: 'Tornar enrere',
        },
        revert: {
            title: 'Desfer canvis',
            confirm: 'Estau segur que voleu desfer els canvis fets al formulari?',
        },
        create: {
            title: 'Crear',
            success: 'Element creat',
        },
        update: {
            title: 'Modificar',
            success: 'Element modificat',
            wrong_resource_type: 'No es possible desar els formularis amb tipus de recurs "{{resourceType}}"',
        },
        delete: {
            title: 'Esborrar',
            confirm: 'Estau segur que voleu esborrar aquest element (aquesta acció no es pot desfer)?',
            success: 'Element esborrat',
        },
        field: {
            reference: {
                open: 'Obrir',
                close: 'Tancar',
                clear: 'Esborrar',
                loading: 'Carregant...',
                noOptions: 'Sense opcions',
                page: 'Mostrant {{size}} de {{totalElements}} elements',
                advanced: {
                    title: 'Seleccionar valor'
                }
            },
            checkboxSelect: {
                true: 'Si',
                false: 'No',
            },
        },
        button: {
            submit: 'Enviar',
            filter: 'Filtrar',
            clear: 'Netejar',
        },
        prevnext: {
            prev: 'Element anterior',
            next: 'Element següent',
        },
    },
    actionReport: {
        confirm: {
            title: 'Confirmació',
            message: 'Estau segur que voleu executar l\'acció "{{action}}"?',
        }
    },
    buttons: {
        answerRequired: {
            accept: 'Acceptar',
            cancel: 'Cancel·lar',
        },
        confirm: {
            accept: 'Acceptar',
            cancel: 'Cancel·lar',
        },
        form: {
            save: 'Desar',
            cancel: 'Cancel·lar',
        },
        action: {
            exec: 'Executar',
            cancel: 'Cancel·lar',
        },
        report: {
            generate: 'Generar',
            cancel: 'Cancel·lar',
        },
        misc: {
            retry: 'Tornar a provar',
        },
    }
};

export default componentsCa;