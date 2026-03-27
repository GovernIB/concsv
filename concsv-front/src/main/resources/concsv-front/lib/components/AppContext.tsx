import React from 'react';
import { PersistentStateReturned } from '../util/usePersistentState';
import { FormFieldCustomProps } from './form/FormField';
import { FormButtonCustomProps } from './form/FormButton';
import { ResourceApiUserSessionValuePair } from './ResourceApiContext';

export type TemporalMessageSeverity = 'success' | 'info' | 'warning' | 'error';
export type TemporalMessageShowFn = (
    title: string | null,
    message: string,
    severity?: TemporalMessageSeverity,
    additionalComponents?: React.ReactElement[]) => void;

export type DialogVariant = 'text' | 'outlined' | 'contained';
export type MessageDialogShowFn = (title: string | null, message: string, buttons: DialogButton[], componentProps?: any) => Promise<string>;
export type DialogButton = {
    value: any;
    text: string;
    icon?: string;
    componentProps?: any;
};

export type AppContextType = {
    getFormFieldComponent: (type?: string) => React.FC<FormFieldCustomProps> | undefined;
    getFormButtonComponent: () => React.FC<FormButtonCustomProps> | undefined;
    setMarginsDisabled: (marginsDisabled: boolean) => void;
    contentExpandsToAvailableHeight: boolean;
    setContentExpandsToAvailableHeight: (expand: boolean) => void;
    getLinkComponent: () => any;
    goBack: (fallback?: string) => void;
    useLocationPath: () => string;
    anyHistoryEntryExist: () => boolean;
    setMessageDialogShow: (fn: MessageDialogShowFn) => void;
    messageDialogShow: MessageDialogShowFn;
    setTemporalMessageShow: (fn: TemporalMessageShowFn) => void;
    temporalMessageShow: TemporalMessageShowFn;
    userSession: any | undefined;
    setUserSessionAttribute: (attribute: string, value: any) => boolean;
    setUserSessionAttributes: (attributeValuePairs: ResourceApiUserSessionValuePair[]) => boolean;
    currentLanguage: string | undefined;
    setCurrentLanguage: (lang?: string | undefined) => void;
    t: (key: string, params?: any) => any;
    persistentStateReady: boolean;
    persistentStateGet: (field?: string) => any;
    persistentStateSet: (field: string, value: any) => void;
    persistentStateRemove: (field: string) => void;
    useDrag?: (fn: () => any, deps?: unknown[]) => any;
    useDrop?: (fn: () => any, deps?: unknown[]) => any;
} & PersistentStateReturned;

export const AppContext = React.createContext<AppContextType | undefined>(undefined);
export const useAppContext = () => {
    const context = React.useContext(AppContext);
    if (context === undefined) {
        throw new Error('useAppContext must be used within an AppProvider');
    }
    return context;
}

export const useOptionalAppContext = (): AppContextType | undefined => {
    return React.useContext(AppContext);
}

export default AppContext;