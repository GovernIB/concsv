import React from 'react';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import Typography from '@mui/material/Typography';
import { BasePage } from 'reactlib';

/**
 * Pantalla provisional dels manteniments que encara no s'han implementat. Les rutes i les
 * entrades de menú ja hi són (amb el seu control d'accés) perquè implementar-ne un només sigui
 * substituir aquest component a router.tsx.
 */
const EnConstruccio: React.FC<{ titol: string }> = ({ titol }) => {
    const { t } = useTranslation();
    return (
        <BasePage>
            <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', mt: 8, gap: 1 }}>
                <Icon sx={{ fontSize: 64 }} color="disabled">construction</Icon>
                <Typography variant="h5">{t(titol)}</Typography>
                <Typography color="text.secondary">{t('page.enConstruccio.missatge')}</Typography>
            </Box>
        </BasePage>
    );
};

export default EnConstruccio;
