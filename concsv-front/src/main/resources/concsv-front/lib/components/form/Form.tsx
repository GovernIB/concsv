import React from 'react';
import { useAppContext } from '../AppContext';
import { useConfirmDialogButtons } from '../AppButtons';
import { useResourceApiService, ResourceApiError } from '../ResourceApiProvider';
import useLogConsole from '../../util/useLogConsole';
import { useReducerWithActionMiddleware } from '../../util/useReducerWithActionMiddleware';
import ResourceApiFormContext, {
    FormApi,
    FormApiRef,
    FormFieldError,
    FormFieldDataAction,
    FormFieldDataActionType,
    FormResourceType,
    useOptionalFormContext
} from './FormContext';

const LOG_PREFIX = 'FORM';

export type FormProps = React.PropsWithChildren & {
    title?: string;
    resourceName: string;
    resourceType?: FormResourceType;
    resourceTypeCode?: string;
    id?: any;
    apiRef?: FormApiRef;
    additionalData?: any;
    perspectives?: string[];
    initOnChangeRequest?: boolean;
    commonFieldComponentProps?: any;
    onDataChange?: (data: any) => void;
    fieldTypeMap?: Map<string, string>;
    inline?: boolean;
    debug?: boolean;
};

const formDataReducer = (state: any, action: FormFieldDataAction): any => {
    const { type, payload } = action;
    switch (type) {
        case FormFieldDataActionType.RESET: {
            return payload;
        }
        case FormFieldDataActionType.FIELD_CHANGE: {
            return {
                ...state,
                ...payload.changes,
                [payload.fieldName]: payload.value
            };
        }
    }
}

const processValidationErrors = (
    error: ResourceApiError,
    setFieldErrors: (fieldErrors?: FormFieldError[]) => void): ResourceApiError | undefined => {
    // Es processen únicament els errors de validació (HTTP status 422)
    if (error.status === 422) {
        const errors = error.body.errors ?? error.body.validationErrors;
        // TODO mostrar globalErrors
        //const globalErrors = errors?.find((e: any) => e.field == null);
        const fieldErrors = errors?.
            filter((e: any) => e.field != null).
            map((e: any) => ({
                code: e.code,
                field: e.field,
                message: e.message,
            }));
        setFieldErrors(fieldErrors);
    } else {
        return error;
    }
}

const getInitialDataFromFields = (fields: any[] | undefined) => {
    const initialDataFromFields: any = {};
    fields?.forEach(f => f.value && (initialDataFromFields[f.name] = f.value));
    return initialDataFromFields;
}

export const useFormApiRef: () => FormApiRef = () => {
    const formApiRef = React.useRef<FormApi | any>({});
    const formContext = useOptionalFormContext();
    return formContext?.apiRef ?? formApiRef;
}

export const Form: React.FC<FormProps> = (props) => {
    const {
        resourceName,
        resourceType,
        resourceTypeCode,
        id,
        apiRef: apiRefProp,
        additionalData,
        perspectives,
        initOnChangeRequest,
        commonFieldComponentProps,
        onDataChange,
        fieldTypeMap,
        inline,
        debug = false,
        children
    } = props;
    const logConsole = useLogConsole(LOG_PREFIX);
    const {
        temporalMessageShow,
        messageDialogShow,
        t,
    } = useAppContext();
    const {
        isReady: apiIsReady,
        currentError: apiCurrentError,
        getOne: apiGetOne,
        fields: apiFields,
        onChange: apiOnChange,
        create: apiCreate,
        update: apiUpdate,
        execAction: apiExecAction,
        generateReport: apiGenerateReport,
        validate: apiValidate,
        currentLinks: apiCurrentLinks
    } = useResourceApiService(resourceName);
    const confirmDialogButtons = useConfirmDialogButtons();
    const confirmDialogComponentProps = { maxWidth: 'sm', fullWidth: true };
    const [isLoading, setIsLoading] = React.useState<boolean>(true);
    const [modified, setModified] = React.useState<boolean>(false);
    const [fields, setFields] = React.useState<any[]>();
    const [fieldErrors, setFieldErrors] = React.useState<FormFieldError[] | undefined>();
    const [revertData, setRevertData] = React.useState<any>();
    const [apiLinks, setApiLinks] = React.useState<any>();
    const apiRef = React.useRef<FormApi>();
    const idFromExternalResetRef = React.useRef<any>();
    const isSaveLinkPresent = apiLinks?.[id != null ? 'update' : 'create'] != null;
    const isDeleteLinkPresent = id && apiLinks?.['delete'] != null;
    const calculatedId = (id?: any) => idFromExternalResetRef.current ?? id;
    const onChangeActionMiddleware = React.useCallback((state: any, action: FormFieldDataAction) => {
        if (action.type === FormFieldDataActionType.FIELD_CHANGE) {
            const { field, fieldName, value: fieldValue } = action.payload;
            if (field?.onChangeActive) {
                return new Promise<FormFieldDataAction>((resolve, reject) => {
                    apiOnChange(calculatedId(id), {
                        fieldName,
                        fieldValue,
                        previous: state,
                    }).then((changes: any) => {
                        resolve({
                            type: action.type,
                            payload: { ...action.payload, changes }
                        });
                    }).catch(reject);
                });
            }
        }
    }, [id, apiOnChange]);
    const [data, dataDispatchAction] = useReducerWithActionMiddleware<FormFieldDataAction>(formDataReducer, {}, onChangeActionMiddleware);
    const getData = () => data;
    const dataGetValue = (callback: (state: any) => any) => callback(data);
    const getInitialData = React.useCallback(async (id: any, fields: any[], additionalData: any, initOnChangeRequest?: boolean): Promise<any> => {
        // Obté les dades inicials
        // Si és un formulari de modificació obté les dades fent una petició al servidor
        // Si és un formulari de creació obté les dades dels camps
        const initialData = id ? await apiGetOne(id, { data: { perspectives }, includeLinks: true }) : getInitialDataFromFields(fields);
        const mergedData = { ...initialData, ...additionalData };
        return initOnChangeRequest ? await apiOnChange(id, { previous: mergedData }) : mergedData;
    }, [apiGetOne, apiOnChange]);
    const processSubmitError = (
        error: ResourceApiError,
        temporalMessageTitle: string,
        reject: (reason: any) => void) => {
        // S'ignoren els errors de tipus cancel·lació
        if (!error.body?.modificationCanceledError) {
            // Quan es produeixen errors es fa un reject de la promesa.
            // Si els errors els tracta el mateix component Form aleshores la
            // cridada a reject es fa amb un valor buit.
            // Si l'error s'ha de mostrar a l'usuari es fa un reject amb l'error.
            const errorToShow = processValidationErrors(error, setFieldErrors);
            if (errorToShow != null) {
                temporalMessageShow(temporalMessageTitle, error.message, 'error');
            }
            reject(errorToShow);
        }
    }
    const reset = (data: any) => {
        // Accions per a reiniciar l'estat del formulari
        dataDispatchAction({
            type: FormFieldDataActionType.RESET,
            payload: data,
        });
        setIsLoading(false);
        setModified(false);
        setRevertData(data);
        setFieldErrors(undefined);
        idFromExternalResetRef.current = null;
    }
    const externalReset = (data?: any, id?: any) => {
        // Versió de reset per a cridar externament mitjançant l'API
        const mergedData = { ...(data ?? getInitialDataFromFields(fields)), ...additionalData };
        if (initOnChangeRequest) {
            apiOnChange(id, { previous: mergedData }).then((changedData: any) => {
                reset(changedData);
                idFromExternalResetRef.current = id;
            });
        } else {
            reset(mergedData);
            idFromExternalResetRef.current = id;
        }
    }
    const revert = (unconfirmed?: boolean) => {
        const revertFn = () => {
            dataDispatchAction({
                type: FormFieldDataActionType.RESET,
                payload: revertData,
            });
            setModified(false);
        }
        if (unconfirmed) {
            revertFn();
        } else {
            messageDialogShow(
                t('form.revert.title'),
                t('form.revert.confirm'),
                confirmDialogButtons,
                confirmDialogComponentProps).
                then((value: any) => {
                    value && revertFn();
                });
        }
    }
    const save = () => new Promise<any>((resolve, reject) => {
        if (resourceType == null) {
            const calcId = calculatedId(id);
            setFieldErrors(undefined);
            const apiAction = calcId != null ? apiUpdate(calcId, { data }) : apiCreate({ data });
            apiAction.
                then((savedData: any) => {
                    const message = calcId != null ? t('form.update.success') : t('form.create.success');
                    temporalMessageShow(null, message, 'success');
                    dataDispatchAction({
                        type: FormFieldDataActionType.RESET,
                        payload: savedData,
                    });
                    resolve(savedData);
                }).
                catch((error: ResourceApiError) => {
                    const title = calcId != null ? t('form.update.error') : t('form.create.error');
                    processSubmitError(error, title, reject)
                });
        } else {
            reject(t('form.update.wrong_resource_type', { resourceType }));
        }
    });
    const execAction = (code: string) => new Promise<any>((resolve, reject) => {
        const calcId = calculatedId(id);
        apiExecAction(calcId, { code, data }).
            then(resolve).
            catch((error: ResourceApiError) => {
                processSubmitError(error, t('form.exec.error'), reject)
            });
    });
    const generateReport = (code: string) => new Promise<Blob>((resolve, reject) => {
        const calcId = calculatedId(id);
        apiGenerateReport(calcId, { code, data }).
            then(resolve).
            catch((error: ResourceApiError) => {
                processSubmitError(error, t('form.generate.error'), reject)
            });
    });
    const validate = () => new Promise<void>((resolve, reject) => {
        apiValidate({ data }).
            then(resolve).
            catch((error: ResourceApiError) => {
                processSubmitError(error, t('form.validate.error'), reject)
            });
    });
    const delette = () => {
        messageDialogShow(
            t('form.delete.title'),
            t('form.delete.confirm'),
            confirmDialogButtons,
            confirmDialogComponentProps).
            then((value: any) => {
                if (value) {
                    // TODO delete
                    temporalMessageShow(null, t('form.delete.success'), 'success');
                }
            });
    }
    React.useEffect(() => {
        if (apiCurrentError) {
            setIsLoading(false);
        }
    }, [apiCurrentError]);
    React.useEffect(() => {
        // Obté els camps pel formulari fent una petició al servidor
        if (apiIsReady) {
            debug && logConsole.debug('Loading fields' + (resourceType ? ' of type' : ''), resourceType, resourceTypeCode);
            const args = resourceType ? { data: { [resourceType]: resourceTypeCode } } : undefined;
            apiFields(args).then((fields: any) => {
                debug && logConsole.debug('Fields loaded', '(' + fields?.length + ')');
                setFields(fields);
            });
            setApiLinks(apiCurrentLinks);
        }
    }, [apiIsReady]);
    React.useEffect(() => {
        // Obté les dades inicials pel formulari
        if (fields) {
            getInitialData(id, fields, additionalData, initOnChangeRequest).
                then((initialData: any) => {
                    debug && logConsole.debug('Initial data loaded', initialData);
                    const { _links: initialDataLinks, ...initialDataWithoutLinks } = initialData;
                    id && setApiLinks(initialDataLinks);
                    reset(initialDataWithoutLinks);
                });
        }
    }, [id, fields]);
    React.useEffect(() => {
        // Controla l'estat de formulari amb modificacions
        !isLoading && setModified(true);
        onDataChange?.(data);
    }, [data]);
    apiRef.current = {
        getData,
        reset: externalReset,
        revert,
        save,
        execAction,
        generateReport,
        validate,
        delete: delette,
    };
    if (apiRefProp) {
        if (apiRefProp.current) {
            apiRefProp.current.getData = getData;
            apiRefProp.current.reset = externalReset;
            apiRefProp.current.revert = revert;
            apiRefProp.current.save = save;
            apiRefProp.current.delete = delette;
        } else {
            logConsole.warn('apiRef prop must be initialized with empty object');
        }
    }
    const context = React.useMemo(() => ({
        id: calculatedId(id),
        resourceName,
        resourceType,
        resourceTypeCode,
        isLoading,
        isReady: !isLoading,
        apiLinks,
        isSaveLinkPresent,
        isDeleteLinkPresent,
        fields,
        fieldErrors,
        fieldTypeMap,
        inline,
        data,
        modified: modified ?? false,
        apiRef,
        dataGetFieldValue: (fieldName: string) => dataGetValue((state) => state?.[fieldName]),
        dataDispatchAction,
        commonFieldComponentProps,
    }), [isLoading, apiLinks, fields, fieldErrors, data, dataDispatchAction, commonFieldComponentProps]);
    return <ResourceApiFormContext.Provider value={context}>
        {!isLoading ? children : null}
    </ResourceApiFormContext.Provider>;
}

export default Form;