import React from 'react';
import { useAppContext } from '../AppContext';
import useLogConsole from '../../util/useLogConsole';
import {
    useFormContext,
    FormFieldDataActionType,
    FormFieldError,
} from './FormContext';

const LOG_PREFIX = 'FIELD';

export type FormFieldComponent = {
    type: string;
    component: React.FC<FormFieldCustomProps>;
};

type FormFieldCommonProps = {
    name: string;
    label?: string;
    inline?: boolean;
    required?: boolean;
    disabled?: boolean;
    readOnly?: boolean;
    onChange?: (value: any) => void;
    componentProps?: any;
};

type FormFieldProps = FormFieldCommonProps & {
    debug?: boolean;
};

type FormFieldRendererProps = FormFieldCommonProps & {
    value?: any;
    field?: any;
    fieldError?: FormFieldError;
    fieldTypeMap?: Map<string, string>;
    onFieldValueChange: (value: any) => void;
    debug?: boolean;
};

export type FormFieldCustomProps = FormFieldCommonProps & {
    label: string;
    value: any;
    field: any;
    fieldError?: FormFieldError;
    onChange: (value: any, fieldName: string) => void;
    style?: React.CSSProperties;
};

const FormFieldRenderer: React.FC<FormFieldRendererProps> = (props) => {
    const {
        name,
        label: labelProp,
        value,
        field,
        fieldError,
        fieldTypeMap,
        inline,
        required,
        disabled,
        readOnly,
        onFieldValueChange,
        componentProps,
        debug,
        ...otherProps
    } = props;
    const { getFormFieldComponent } = useAppContext();
    const logConsole = useLogConsole(LOG_PREFIX);
    const label = labelProp ?? (field ? field.prompt : name);
    debug && logConsole.debug('Field', name, 'rendered', (value ? 'with value: ' + value : 'empty'));
    const mappedFieldType = fieldTypeMap?.get(field?.type) ?? field?.type;
    const FormFieldComponent: React.FC<FormFieldCustomProps> | undefined = field ? getFormFieldComponent(mappedFieldType) : undefined;
    return FormFieldComponent ? <FormFieldComponent
        name={name}
        label={label}
        value={value}
        field={field}
        fieldError={fieldError}
        inline={inline}
        required={required}
        disabled={disabled}
        readOnly={readOnly}
        onChange={onFieldValueChange}
        componentProps={componentProps}
        {...otherProps} /> : <span>[&nbsp;Unknown field: {name}&nbsp;]</span>;
}

export const FormField: React.FC<FormFieldProps & any> = (props) => {
    const {
        name,
        inline: inlineProp,
        required,
        disabled,
        readOnly,
        onChange,
        componentProps,
        debug,
        ...otherProps
    } = props;
    const [field, setField] = React.useState<any>();
    const [fieldError, setFieldError] = React.useState<FormFieldError | undefined>();
    const {
        isReady: isFormReady,
        isSaveLinkPresent,
        fields,
        fieldErrors,
        fieldTypeMap,
        inline: inlineCtx,
        dataGetFieldValue,
        dataDispatchAction,
        commonFieldComponentProps,
    } = useFormContext();
    React.useEffect(() => {
        if (fields) {
            const field = fields.find(f => f.name === name);
            setField(field ?? null);
        }
    }, [fields]);
    React.useEffect(() => {
        if (fieldErrors) {
            const fieldError = fieldErrors.find(e => e.field === name);
            setFieldError(fieldError ?? undefined);
        }
    }, [fieldErrors]);
    const isReady = isFormReady && field !== undefined;
    const value = dataGetFieldValue(name);
    const handleFieldValueChange = (value: any) => {
        dataDispatchAction({
            type: FormFieldDataActionType.FIELD_CHANGE,
            payload: { fieldName: name, field, value }
        });
        onChange?.(value, name);
    }
    const inline = inlineProp ?? inlineCtx;
    const renderer = React.useMemo(() => {
        return <FormFieldRenderer
            name={name}
            value={value}
            field={field}
            fieldError={fieldError}
            fieldTypeMap={fieldTypeMap}
            inline={inline}
            required={required}
            disabled={!isSaveLinkPresent || disabled}
            readOnly={!isSaveLinkPresent || readOnly}
            componentProps={{
                ...commonFieldComponentProps,
                ...componentProps,
            }}
            debug={debug}
            onFieldValueChange={handleFieldValueChange}
            {...otherProps} />;
    }, [
        name,
        inline,
        required,
        disabled,
        readOnly,
        isSaveLinkPresent,
        field,
        fieldError,
        fieldTypeMap,
        value,
        componentProps,
        debug,
        dataDispatchAction,
        commonFieldComponentProps
    ]);
    return isReady ? renderer : null;
}

export default FormField;