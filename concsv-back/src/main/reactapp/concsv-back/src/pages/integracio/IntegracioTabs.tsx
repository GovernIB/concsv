import React from 'react';
import { useTranslation } from 'react-i18next';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';

export type IntegracioCodi = 'ARXIU' | 'CUSTODIA' | 'VALIDACIO_FIRMES' | 'CONVERSIO';

/** Integracions que es poden consultar, en l'ordre de les pestanyes (veure IntegracioCodiEnum). */
export const INTEGRACIO_CODIS: IntegracioCodi[] = ['ARXIU', 'CUSTODIA', 'VALIDACIO_FIRMES', 'CONVERSIO'];

/**
 * Pestanyes del monitor d'integracions: una per integració. La pestanya seleccionada s'afegeix com
 * "named query" del llistat, que el backend tradueix a una condició sobre el camp `codi`.
 */
export const useIntegracioTabs = () => {
    const { t } = useTranslation();
    const [value, setValue] = React.useState<IntegracioCodi>(INTEGRACIO_CODIS[0]);

    const handleChange = (_event: React.SyntheticEvent, newValue: IntegracioCodi) => {
        setValue(newValue);
    };

    const tabElement = (
        <Tabs value={value} onChange={handleChange} variant="scrollable" sx={{ px: 1 }}>
            {INTEGRACIO_CODIS.map((codi) => (
                <Tab
                    key={codi}
                    value={codi}
                    label={t(`page.integracio.pipella.${codi}`)}
                    sx={{ textTransform: 'none' }}
                />
            ))}
        </Tabs>
    );

    return { value, tabElement };
};

export default useIntegracioTabs;
