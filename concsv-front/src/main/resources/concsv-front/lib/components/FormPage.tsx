import React from 'react';
import { useAppContext } from './AppContext';

type FormPageProps = React.PropsWithChildren & {
    disableMargins?: true;
};

export const FormPage: React.FC<FormPageProps> = (props) => {
    const { disableMargins = true, children } = props;
    const { setMarginsDisabled } = useAppContext();
    React.useEffect(() => {
        setMarginsDisabled(disableMargins);
        return () => setMarginsDisabled(false);
    }, [disableMargins]);
    return children;
}

export default FormPage;