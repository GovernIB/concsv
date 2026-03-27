import React from 'react';
import { useAppContext } from '../AppContext';
import { useOptionalFormContext } from './FormContext';
import { useOptionalFilterContext } from './FilterContext';
import FormButtonDefault from './FormButtonDefault';

export type FormButtonCustomProps = FormButtonProps & {
    text: string;
    icon?: string;
    onClick: () => void;
};

type FormButtonProps = {
    type: 'submit' | 'filter' | 'clear';
    icon?: string;
    componentProps?: any;
};

export const FormButton: React.FC<FormButtonProps> = (props) => {
    const {
        type,
        componentProps,
        ...otherProps
    } = props;
    const { t, getFormButtonComponent } = useAppContext();
    const {apiRef: formApiRef} = useOptionalFormContext() ?? {};
    const {apiRef: filterApiRef} = useOptionalFilterContext() ?? {};
    const handleFormButtonClick = () => {
        if (type === 'submit') {
            if (formApiRef) {
                formApiRef.current?.save();
            } else {
                console.warn('[FormButton] Submit action must be placed within a FormProvider');
            }
        } else if (type === 'clear') {
            if (filterApiRef) {
                filterApiRef.current?.clear();
            } else if (formApiRef) {
                formApiRef.current?.revert(true);
            } else {
                console.warn('[FormButton] Clear action must be placed within a FormProvider');
            }
        } else if (type === 'filter') {
            if (filterApiRef) {
                filterApiRef.current?.filter();
            } else {
                console.warn('[FormButton] Filter action must be placed within a FilterProvider');
            }
        }
    }
    const FormButtonComponent = getFormButtonComponent?.() ?? FormButtonDefault;
    return <FormButtonComponent
        type={type}
        text={t('form.button.' + type)}
        onClick={handleFormButtonClick}
        componentProps={componentProps}
        {...otherProps} />;
}
export default FormButton;