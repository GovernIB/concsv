import React from 'react';
import { useAppContext } from './AppContext';

type GridPageProps = React.PropsWithChildren & {
    disableMargins?: true;
};

export const GridPage: React.FC<GridPageProps> = (props) => {
    const { disableMargins = false, children } = props;
    const {
        setMarginsDisabled,
        setContentExpandsToAvailableHeight
    } = useAppContext();
    React.useEffect(() => {
        setMarginsDisabled(disableMargins);
        return () => setMarginsDisabled(false);
    }, [disableMargins]);
    React.useEffect(() => {
        setContentExpandsToAvailableHeight(true);
        return () => setContentExpandsToAvailableHeight(false);
    }, []);
    return <div style={{
        display: 'flex',
        flexDirection: 'column',
        height: '100%',
    }}>
        {children}
    </div>;
}

export default GridPage;