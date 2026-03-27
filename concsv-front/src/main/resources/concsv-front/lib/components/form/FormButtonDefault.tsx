import React from 'react';
import { FormButtonCustomProps } from './FormButton';

export const ResourceApiFormFieldDefault: React.FC<FormButtonCustomProps> = (props) => {
    const {
        type,
        text,
        onClick,
        componentProps,
    } = props;
    return <button
        type={type}
        onClick={onClick}
        {...componentProps}>
        {text}
    </button>;
}
export default ResourceApiFormFieldDefault;