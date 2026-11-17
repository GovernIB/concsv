import React from 'react';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import { Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { tonal, type TonalColor } from '../../util/tonal';

type DocumentExclosEstatProps = {
    /** Estat de la darrera consulta (OK, NO_TROBAT, ERROR); buit si encara no s'ha consultat. */
    value?: string | null;
    /** Motiu del no trobat o de l'error: es mostra com a títol. */
    missatge?: string | null;
};

const etiquetaSx = {
    display: 'flex',
    alignItems: 'center',
    gap: 0.5,
    p: '4px',
    px: 1,
    borderRadius: '4px',
};

const ESTATS: Record<string, { color: TonalColor; icon: string }> = {
    OK: { color: 'success', icon: 'done' },
    NO_TROBAT: { color: 'warning', icon: 'search_off' },
    ERROR: { color: 'error', icon: 'warning' },
};

const ESTAT_PENDENT: { color: TonalColor; icon: string } = { color: 'info', icon: 'schedule' };

/**
 * Insígnia de l'estat de les dades d'un document exclòs: verda (consultat), groga (no trobat), vermella
 * (error, amb el missatge com a títol) o blava (pendent de consultar).
 */
export const DocumentExclosEstat: React.FC<DocumentExclosEstatProps> = ({ value, missatge }) => {
    const { t } = useTranslation();
    const coneguda = value ? ESTATS[value] : undefined;
    const estat = coneguda ?? ESTAT_PENDENT;
    const clau = coneguda ? value : 'PENDENT';

    return (
        <Box title={missatge ?? undefined} sx={[etiquetaSx, tonal(estat.color)]}>
            <Icon fontSize="small" sx={{ fontSize: '18px' }}>
                {estat.icon}
            </Icon>
            <Typography variant="caption">{t(`page.documentsExclosos.estat.${clau}`)}</Typography>
        </Box>
    );
};

export default DocumentExclosEstat;
