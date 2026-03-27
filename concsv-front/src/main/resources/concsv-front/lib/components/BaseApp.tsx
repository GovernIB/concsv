import React from 'react';
import componentsCa from '../i18n/componentsCa';
import componentsEn from '../i18n/componentsEn';
import componentsEs from '../i18n/componentsEs';
import { usePersistentState } from '../util/usePersistentState';
import { FormFieldComponent } from './form/FormField';
import { FormButtonCustomProps } from './form/FormButton';
import { ResourceApiFormFieldDefault } from './form/FormFieldDefault';
import {
    AppContext,
    MessageDialogShowFn,
    DialogButton,
    TemporalMessageShowFn,
    TemporalMessageSeverity,
} from './AppContext';
import { useResourceApiContext } from './ResourceApiContext';
import { ResourceApiUserSessionValuePair } from './ResourceApiContext';

export const MARGIN_UNIT_PX = 8;
export const LIB_I18N_NS = 'base-react';
export const PERSISTENT_LANGUAGE_KEY = 'lang';
export const PERSISTENT_SESSION_KEY = 'user-session';

type I18nHandleLanguageChangeFn = (lang?: string) => void;
type I18nAddResourceBundleCallback = (lang: string, ns: string, bundle: any) => void;

export type BaseAppProps = React.PropsWithChildren & {
    code: string;
    persistentSession?: boolean;
    persistentLanguage?: boolean;
    i18nUseTranslation: (ns: string) => { t: any };
    i18nCurrentLanguage?: string;
    i18nHandleLanguageChange?: I18nHandleLanguageChangeFn;
    i18nAddResourceBundleCallback?: I18nAddResourceBundleCallback;
    routerGoBack: (fallback?: string) => void;
    routerAnyHistoryEntryExist: () => boolean;
    routerUseLocationPath: () => string;
    linkComponent: React.ElementType;
    dragDropUseDrag?: (fn: () => any, deps?: unknown[]) => any;
    dragDropUseDrop?: (fn: () => any, deps?: unknown[]) => any;
    formFieldComponents?: FormFieldComponent[];
    formButtonComponent?: React.FC<FormButtonCustomProps>;
    contentComponent?: React.ElementType;
    contentComponentProps?: any;
    contentComponentDisabled?: boolean;
};

export type BaseAppContentComponentProps = React.PropsWithChildren & {
    offline: boolean;
    appReady: boolean;
    marginsDisabled: boolean;
    contentExpandsToAvailableHeight: boolean;
    appbarComponent?: React.ReactElement;
    menuComponent?: React.ReactElement;
    offlineComponent?: React.ReactElement;
    headerHeight?: number;
};

const useDialog = () => {
    const dialogShowFn = React.useRef<MessageDialogShowFn>();
    const setMessageDialogShow = (fn: MessageDialogShowFn) => {
        dialogShowFn.current = fn;
    }
    const messageDialogShow: MessageDialogShowFn = (title: string | null, message: string, buttons: DialogButton[], componentProps?: any) => {
        if (dialogShowFn.current) {
            return dialogShowFn.current(title, message, buttons, componentProps);
        } else {
            console.warn('Dialog component not configured in BaseApp');
            return new Promise((_resolve, reject) => reject());
        }
    }
    return {
        setMessageDialogShow,
        messageDialogShow,
    };
}

const useTemporalMessage = () => {
    const temporalMessageShowFn = React.useRef<TemporalMessageShowFn>();
    const setTemporalMessageShow = (fn: TemporalMessageShowFn) => {
        temporalMessageShowFn.current = fn;
    }
    const temporalMessageShow: TemporalMessageShowFn = (
        title: string | null,
        message: string,
        severity?: TemporalMessageSeverity,
        additionalComponents?: React.ReactElement[]) => {
        if (temporalMessageShowFn.current) {
            temporalMessageShowFn.current(title, message, severity, additionalComponents);
        } else {
            console.warn('Temporal message component not configured in BaseApp');
        }
    }
    return {
        setTemporalMessageShow,
        temporalMessageShow,
    };
}

const useFormFieldComponents = (formFieldComponents?: FormFieldComponent[]) => {
    const formFieldComponentsMap: any = {};
    formFieldComponents?.forEach(ffc => {
        formFieldComponentsMap[ffc.type] = ffc.component;
    });
    const formFieldComponentsRef = React.useRef<any>(formFieldComponentsMap);
    const getFormFieldComponent = (type?: string) => {
        if (type && formFieldComponentsRef.current && formFieldComponentsRef.current[type]) {
            return formFieldComponentsRef.current[type];
        } else {
            console.warn('Form field type ' + type + ' not found, using default')
            return ResourceApiFormFieldDefault;
        }
    }
    return getFormFieldComponent;
}

const useI18n = (
    code: string,
    persistentLanguage: boolean,
    i18nUseTranslation: (ns: string) => { t: any },
    i18nCurrentLanguage?: string,
    i18nHandleLanguageChange?: I18nHandleLanguageChangeFn,
    i18nAddResourceBundleCallback?: I18nAddResourceBundleCallback) => {
    const {
        persistentStateReady,
        persistentStateGet,
        persistentStateSet,
    } = usePersistentState(code);
    const { t: tI18Next } = i18nUseTranslation(LIB_I18N_NS);
    const {
        currentLanguage,
        setCurrentLanguage,
    } = useResourceApiContext();
    React.useEffect(() => {
        i18nAddResourceBundleCallback?.('ca', LIB_I18N_NS, componentsCa);
        i18nAddResourceBundleCallback?.('es', LIB_I18N_NS, componentsEs);
        i18nAddResourceBundleCallback?.('en', LIB_I18N_NS, componentsEn);
    }, []);
    React.useEffect(() => {
        if (persistentLanguage && persistentStateReady && currentLanguage == null) {
            const lang = persistentStateGet(PERSISTENT_LANGUAGE_KEY);
            setCurrentLanguage(lang ?? '');
        }
    }, [persistentStateReady]);
    React.useEffect(() => {
        if (persistentLanguage && persistentStateReady) {
            persistentStateSet(PERSISTENT_LANGUAGE_KEY, currentLanguage);
        }
    }, [currentLanguage]);
    React.useEffect(() => {
        i18nCurrentLanguage && setCurrentLanguage(i18nCurrentLanguage);
    }, [i18nCurrentLanguage]);
    React.useEffect(() => {
        currentLanguage && i18nHandleLanguageChange?.(currentLanguage);
    }, [currentLanguage]);
    const t = (key: string, params?: any) => tI18Next(key, params);
    return {
        currentLanguage,
        setCurrentLanguage,
        t
    };
}

const useUserSession = (code: string, persistentSession: boolean) => {
    const {
        userSession,
        setUserSession,
        setUserSessionAttributes
    } = useResourceApiContext();
    const {
        persistentStateReady,
        persistentStateGet,
        persistentStateSet,
        persistentStateRemove,
    } = usePersistentState(code);
    React.useEffect(() => {
        if (persistentSession && persistentStateReady && userSession == null) {
            const session = persistentStateGet(PERSISTENT_SESSION_KEY);
            setUserSession(session ?? {});
        }
    }, [persistentStateReady]);
    React.useEffect(() => {
        if (persistentSession && persistentStateReady) {
            persistentStateSet(PERSISTENT_SESSION_KEY, userSession);
        }
    }, [userSession]);
    const localSetUserSessionAttribute = (attribute: string, value: any): boolean => {
        return localSetUserSessionAttributes([{ attribute, value }])
    }
    const localSetUserSessionAttributes = (attributeValuePairs: ResourceApiUserSessionValuePair[]): boolean => {
        if (persistentSession) {
            const session = persistentStateGet(PERSISTENT_SESSION_KEY);
            const changes: any = {};
            attributeValuePairs.forEach(c => changes[c.attribute] = c.value);
            persistentStateSet(PERSISTENT_SESSION_KEY, { ...session, ...changes });
        }
        return setUserSessionAttributes(attributeValuePairs);
    }
    return {
        userSession,
        setUserSessionAttribute: localSetUserSessionAttribute,
        setUserSessionAttributes: localSetUserSessionAttributes,
        persistentStateReady,
        persistentStateGet,
        persistentStateSet,
        persistentStateRemove
    };
}

const ContentComponentDefault: React.FC<BaseAppContentComponentProps> = (props) => {
    const {
        offline,
        appReady,
        marginsDisabled,
        contentExpandsToAvailableHeight,
        appbarComponent,
        menuComponent,
        offlineComponent,
        children,
    } = props;
    const margins = {
        marginTop: 1 * MARGIN_UNIT_PX + 'px',
        marginLeft: 2 * MARGIN_UNIT_PX + 'px',
        marginRight: 2 * MARGIN_UNIT_PX + 'px',
        marginBottom: 0
    };
    const mainBoxHeight = contentExpandsToAvailableHeight ? '100vh' : undefined;
    const childrenOrOfflineComponent = !offline ? children : offlineComponent;
    return <div style={{ display: 'flex', flexDirection: 'column', height: mainBoxHeight }}>
        {appbarComponent}
        <div style={{
            display: 'flex',
            flexGrow: 1,
            minHeight: 0,
            ...(!marginsDisabled ? margins : null)
        }}>
            {menuComponent}
            <main style={{
                flexGrow: 1,
                minWidth: 0,
            }}>
                {appReady ? childrenOrOfflineComponent : null}
            </main>
        </div>
    </div>;
}

export const BaseApp: React.FC<BaseAppProps> = (props) => {
    const {
        code,
        persistentSession,
        persistentLanguage,
        i18nUseTranslation,
        i18nCurrentLanguage,
        i18nHandleLanguageChange,
        i18nAddResourceBundleCallback,
        routerGoBack,
        routerUseLocationPath,
        routerAnyHistoryEntryExist,
        linkComponent,
        dragDropUseDrag,
        dragDropUseDrop,
        formFieldComponents,
        formButtonComponent,
        contentComponent,
        contentComponentProps,
        contentComponentDisabled,
        children,
    } = props;
    const { offline } = useResourceApiContext();
    const [marginsDisabled, setMarginsDisabled] = React.useState<boolean>(false);
    const [contentExpandsToAvailableHeight, setContentExpandsToAvailableHeight] = React.useState<boolean>(false);
    const getLinkComponent = () => linkComponent;
    const getFormButtonComponent = () => formButtonComponent;
    const {
        setMessageDialogShow,
        messageDialogShow,
    } = useDialog();
    const {
        setTemporalMessageShow,
        temporalMessageShow
    } = useTemporalMessage();
    const {
        currentLanguage,
        setCurrentLanguage,
        t,
    } = useI18n(
        code,
        persistentLanguage ?? false,
        i18nUseTranslation,
        i18nCurrentLanguage,
        i18nHandleLanguageChange,
        i18nAddResourceBundleCallback);
    const getFormFieldComponent = useFormFieldComponents(formFieldComponents);
    const {
        userSession,
        setUserSessionAttribute,
        setUserSessionAttributes,
        persistentStateReady,
        persistentStateGet,
        persistentStateSet,
        persistentStateRemove
    } = useUserSession(code, persistentSession ?? false);
    const context = {
        getFormFieldComponent,
        getFormButtonComponent,
        setMarginsDisabled,
        contentExpandsToAvailableHeight,
        setContentExpandsToAvailableHeight,
        getLinkComponent,
        goBack: routerGoBack,
        anyHistoryEntryExist: routerAnyHistoryEntryExist,
        useLocationPath: routerUseLocationPath,
        setMessageDialogShow,
        messageDialogShow,
        setTemporalMessageShow,
        temporalMessageShow,
        userSession,
        setUserSessionAttribute,
        setUserSessionAttributes,
        currentLanguage,
        setCurrentLanguage,
        t,
        persistentStateReady,
        persistentStateGet,
        persistentStateSet,
        persistentStateRemove,
        useDrag: dragDropUseDrag,
        useDrop: dragDropUseDrop,
    };
    const sessionReady = !persistentSession || userSession != null;
    const languageReady = !persistentLanguage || currentLanguage != null;
    const appReady = sessionReady && languageReady;
    const ProcessedContentComponent = contentComponent ?? ContentComponentDefault;
    return <AppContext.Provider value={context}>
        {contentComponentDisabled ? (appReady ? children : null) : <ProcessedContentComponent
            offline={offline}
            appReady={appReady}
            marginsDisabled={marginsDisabled}
            contentExpandsToAvailableHeight={contentExpandsToAvailableHeight}
            {...contentComponentProps}>
            {children}
        </ProcessedContentComponent>}
    </AppContext.Provider>;
}

export default BaseApp;