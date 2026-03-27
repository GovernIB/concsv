import React from 'react';

export type FormApi = {
    getData: () => any; 
    reset: (data?: any, id?: any) => void;
    revert: (unconfirmed?: boolean) => void;
    save: () => Promise<any>;
    execAction: (code: string) => Promise<any>;
    generateReport: (code: string) => Promise<Blob>;
    validate: () => Promise<void>;
    delete: () => void;
};

export type FormApiRef = React.MutableRefObject<FormApi>;

export enum FormFieldDataActionType {
    RESET = 'RESET',
    FIELD_CHANGE = 'FIELD_CHANGE',
};

export type FormFieldDataActionPayload = {
    field: any;
    fieldName: string;
    value: any;
    changes?: any;
};

export type FormFieldDataAction = {
    type: FormFieldDataActionType;
    payload: FormFieldDataActionPayload;
};

export type FormResourceType = 'action' | 'report' | 'filter';

export type FormContextType = {
    id?:any;
    resourceName: string;
    resourceType?: FormResourceType;
    resourceTypeCode?: string;
    isLoading: boolean;
    isReady: boolean;
    apiLinks?: any;
    isSaveLinkPresent: boolean;
    isDeleteLinkPresent: boolean;
    fields?: any[];
    fieldErrors?: FormFieldError[];
    fieldTypeMap?: Map<string, string>;
    inline?: boolean;
    data?: any;
    modified: boolean;
    apiRef: React.MutableRefObject<FormApi | undefined>;
    dataGetFieldValue: (fieldName: string) => any;
    dataDispatchAction: (action: FormFieldDataAction) => void;
    commonFieldComponentProps?: any;
};

export type FormFieldError = {
    code: string;
    field: string;
    message: string;
};

export const FormContext = React.createContext<FormContextType | undefined>(undefined);

export const useFormContext = () => {
    const context = React.useContext(FormContext);
    if (context === undefined) {
        throw new Error('useFormContext must be used within a FormProvider');
    }
    return context;
}

export const useOptionalFormContext = (): FormContextType | undefined => {
    return React.useContext(FormContext);
}

export default FormContext;