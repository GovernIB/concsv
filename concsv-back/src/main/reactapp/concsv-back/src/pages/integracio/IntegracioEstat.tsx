import React from 'react';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import { Typography } from '@mui/material';

type IntegracioEstatProps = {
    value?: string;
    excepcioMessage?: string;
};

/** Insígnia de l'estat d'una petició: verda per a OK i vermella per a ERROR (amb el missatge de l'excepció com a títol). */
export const IntegracioEstat: React.FC<IntegracioEstatProps> = ({ value, excepcioMessage }) => {
    if (value === 'OK') {
        return (
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    p: '4px',
                    px: 1,
                    color: 'success.contrastText',
                    backgroundColor: 'success.main',
                    borderRadius: '4px',
                }}
            >
                <Icon fontSize="small" sx={{ fontSize: '18px' }}>
                    done
                </Icon>
                <Typography variant={'caption'}>{value}</Typography>
            </Box>
        );
    }
    if (value === 'ERROR') {
        return (
            <Box
                title={excepcioMessage}
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 0.5,
                    p: '4px',
                    px: 1,
                    color: 'error.contrastText',
                    backgroundColor: 'error.main',
                    borderRadius: '4px',
                }}
            >
                <Icon fontSize="small" sx={{ fontSize: '18px' }}>
                    warning
                </Icon>
                <Typography variant={'caption'}>{value}</Typography>
            </Box>
        );
    }
    return <>{value}</>;
};

export default IntegracioEstat;
