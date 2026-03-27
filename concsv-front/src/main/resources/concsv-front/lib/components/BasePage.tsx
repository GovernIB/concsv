import React from 'react';
import { MARGIN_UNIT_PX } from './BaseApp';

export const BasePage: React.FC<React.PropsWithChildren> = (props) => {
    const { children } = props;
    return <div style={{
        marginTop: 1 * MARGIN_UNIT_PX + 'px',
        marginLeft: 3 * MARGIN_UNIT_PX + 'px',
        marginRight: 3 * MARGIN_UNIT_PX + 'px',
        marginBottom: 0 * MARGIN_UNIT_PX + 'px',
    }}>
        {children}
    </div>;
}

export default BasePage;