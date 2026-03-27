const componentsEs = {
    baseapp: {
        menu: {
            home: 'Inicio',
        },
        auth: {
            logout: 'Cerrar sesión',
        },
        error: {
            '404': 'No encontrado',
            '503': 'Sin connexión',
        },
        offline: {
            message: 'Sin conexión con el servidor',
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
                title: 'Borrar elemento',
                confirm: '¿Está seguro de que quiere borrar este elemento (esta acción no se puede deshacer)?',
                success: 'Elemento borrado',
            },
            multiple: {
                title: 'Borrar múltiples elementos',
                confirm: '¿Está seguro de que quiere borrar estos {{count}} elementos (esta acción no se puede deshacer)?',
                success: '{{count}} elementos borrados',
                errors: '{{successCount}} elementos borrados de {{totalCount}}',
            },
        },
        refresh: {
            title: 'Refrescar',
        },
        details: {
            title: 'Detalles',
        },
        selection: {
            one: '1 fila seleccionada',
            multiple: '{{count}} filas seleccionadas',
        },
        error: {
            toolbar: 'Se ha producido un error',
            validation: 'Se han producido errores de validación',
        },
        edit: {
            create: 'Crear',
            update: 'Modificar',
            save: 'Guardar',
            cancel: 'Cancelar'
        },
        tree: {
            header: {
                name: 'Grupo',
                expandAll: 'Expandir',
                expandNone: 'Contraer',
            }
        },
    },
    form: {
        goBack: {
            title: 'Ir atrás',
        },
        revert: {
            title: 'Deshacer cambios',
            confirm: '¿Está seguro de que quiere deshacer los cambios hechos al formulario?',
        },
        create: {
            title: 'Crear',
            success: 'Elemento creado',
        },
        update: {
            title: 'Modificar',
            success: 'Elemento modificado',
            wrong_resource_type: 'No es posible guardar los formularios con tipo de recurso "{{resourceType}}"',
        },
        delete: {
            title: 'Borrar',
            confirm: '¿Está seguro de que quiere borrar este elemento (esta acción no se puede deshacer)?',
            success: 'Elemento borrado',
        },
        field: {
            reference: {
                open: 'Abrir',
                close: 'Cerrar',
                clear: 'Borrar',
                loading: 'Cargando...',
                noOptions: 'Sin opciones',
                page: 'Mostrando {{size}} de {{totalElements}} elementos',
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
            clear: 'Limpiar',
            filter: 'Filtrar',
        },
        prevnext: {
            prev: 'Elemento anterior',
            next: 'Elemento siguiente',
        },
    },
    actionReport: {
        confirm: {
            title: 'Confirmación',
            message: '¿Está seguro de que quiere ejecutar la acción "{{action}}"?',
        }
    },
    buttons: {
        answerRequired: {
            accept: 'Aceptar',
            cancel: 'Cancelar',
        },
        confirm: {
            accept: 'Aceptar',
            cancel: 'Cancelar',
        },
        form: {
            save: 'Guardar',
            cancel: 'Cancelar',
        },
        action: {
            exec: 'Ejecutar',
            cancel: 'Cancelar',
        },
        report: {
            generate: 'Generar',
            cancel: 'Cancelar',
        },
        misc: {
            retry: 'Reintentar',
        },
    },
};

export default componentsEs;