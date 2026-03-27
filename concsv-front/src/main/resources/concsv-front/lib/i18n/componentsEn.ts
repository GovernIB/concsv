const componentsEn = {
    baseapp: {
        menu: {
            home: 'Home',
        },
        auth: {
            logout: 'Log out',
        },
        error: {
            '404': 'Not found',
            '503': 'No connection',
        },
        offline: {
            message: 'Server connection lost',
        }
    },
    grid: {
        export: {
            title: 'Export',
        },
        create: {
            title: 'Create',
        },
        update: {
            title: 'Update',
        },
        delete: {
            single: {
                title: 'Delete item',
                confirm: 'Are you sure you want to delete this item (this action can not be undone)?',
                success: 'Deleted items',
            },
            multiple: {
                title: 'Delete multiple items',
                confirm: 'Are you sure you want to delete {{count}} items (this action can not be undone)?',
                success: '{{count}} deleted items',
                errors: '{{successCount}} deleted items of {{totalCount}}',
            },
        },
        refresh: {
            title: 'Refresh',
        },
        details: {
            title: 'Details',
        },
        selection: {
            one: '1 row selected',
            multiple: '{{count}} rows selected',
        },
        error: {
            toolbar: 'Error happened',
            validation: 'There are validation errors',
        },
        edit: {
            create: 'Create',
            update: 'Update',
            save: 'Save',
            cancel: 'Cancel'
        },
        tree: {
            header: {
                name: 'Group',
                expandAll: 'Expand',
                expandNone: 'Contract',
            }
        },
    },
    form: {
        goBack: {
            title: 'Go back',
        },
        revert: {
            title: 'Undo changes',
            confirm: 'Are you sure you want to revert the changes in the form?',
        },
        create: {
            title: 'Create',
            success: 'Element created',
        },
        update: {
            title: 'Update',
            success: 'Element updated',
            wrong_resource_type: 'Couldn\'t save forms with resource type "{{resourceType}}"',
        },
        delete: {
            title: 'Delete',
            confirm: 'Are you sure you want to delete this item (this action can not be undone)?',
            success: 'Element deleted',
        },
        field: {
            reference: {
                open: 'Open',
                close: 'Close',
                clear: 'Clear',
                loading: 'Loading...',
                noOptions: 'No options',
                page: 'Showing {{size}} out of {{totalElements}} elements',
                advanced: {
                    title: 'Select value'
                }
            },
            checkboxSelect: {
                true: 'Yes',
                false: 'No',
            },
        },
        button: {
            submit: 'Submit',
            clear: 'Clear',
            filter: 'Filter',
        },
        prevnext: {
            prev: 'Previous element',
            next: 'Next element',
        },
    },
    actionReport: {
        confirm: {
            title: 'Confirmation',
            message: 'Are you sure you want to execute action "{{action}}"?',
        }
    },
    buttons: {
        answerRequired: {
            accept: 'Accept',
            cancel: 'Cancel',
        },
        confirm: {
            accept: 'Accept',
            cancel: 'Cancel',
        },
        form: {
            save: 'Save',
            cancel: 'Cancel',
        },
        action: {
            exec: 'Execute',
            cancel: 'Cancel',
        },
        report: {
            generate: 'Generate',
            cancel: 'Cancel',
        },
        misc: {
            retry: 'Retry',
        },
    },
};

export default componentsEn;