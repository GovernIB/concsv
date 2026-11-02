import React from 'react';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import { Typography } from '@mui/material';
import { tonal } from '../../util/tonal';

type IntegracioEstatProps = {
    value?: string;
    excepcioMessage?: string;
};

const etiquetaSx = {
    display: 'flex',
    alignItems: 'center',
    gap: 0.5,
    p: '4px',
    px: 1,
    borderRadius: '4px',
};

/** Literal de l'estat amb només la inicial en majúscula (OK → Ok, ERROR → Error). */
const capitalitza = (value: string) => value.charAt(0) + value.slice(1).toLowerCase();

/**
 * Insígnia de l'estat d'una petició, en color tonal: verda per a OK i vermella per a ERROR (amb el missatge de
 * l'excepció com a títol).
 */
export const IntegracioEstat: React.FC<IntegracioEstatProps> = ({ value, excepcioMessage }) => {
    if (value === 'OK') {
        return (
            <Box sx={[etiquetaSx, tonal('success')]}>
                <Icon fontSize="small" sx={{ fontSize: '18px' }}>
                    done
                </Icon>
                <Typography variant={'caption'}>{capitalitza(value)}</Typography>
            </Box>
        );
    }
    if (value === 'ERROR') {
        return (
            <Box title={excepcioMessage} sx={[etiquetaSx, tonal('error')]}>
                <Icon fontSize="small" sx={{ fontSize: '18px' }}>
                    warning
                </Icon>
                <Typography variant={'caption'}>{capitalitza(value)}</Typography>
            </Box>
        );
    }
    return <>{value}</>;
};

export default IntegracioEstat;
