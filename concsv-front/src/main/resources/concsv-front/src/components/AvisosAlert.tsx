import React from 'react';
import { useTranslation } from 'react-i18next';
import { useAppContext } from '@programari-limit/base-react';
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Collapse from '@mui/material/Collapse';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import { getAvisNivell, useAvisos, type AvisNivell } from './AvisosContext';

/**
 * Banner dels avisos actius, damunt del contingut de cada pàgina. L'assumpte sempre és visible, amb
 * el color del nivell, i el missatge es desplega en fer-hi clic. Cada avís es pot minimitzar amb la
 * X: deixa de sortir aquí però no desapareix, es recupera des dels chips de la capçalera. 
 * En recarregar la pàgina tornen a sortir tots.
 *
 * Els avisos els consulta {@link ./AvisosProvider.tsx}.
 */

const NIVELL_SEVERITY: Record<AvisNivell, 'info' | 'warning' | 'error'> = {
    INFO: 'info',
    WARNING: 'warning',
    ERROR: 'error',
};

const AvisosAlert: React.FC = () => {
    const { t } = useTranslation();
    const { currentLanguage } = useAppContext();
    const { visibles: avisos, minimitza } = useAvisos();
    const [expandedId, setExpandedId] = React.useState<number | null>(null);

    const handleToggleExpand = (id: number) => {
        setExpandedId(expandedId === id ? null : id);
    };

    if (avisos.length === 0) {
        return null;
    }

    return (
        <Box sx={{ width: '100%', mb: 2, display: 'flex', flexDirection: 'column', gap: '2px' }}>
            {avisos.map((aviso) => (
                <Alert
                    key={aviso.id}
                    severity={NIVELL_SEVERITY[getAvisNivell(aviso)]}
                    sx={{
                        py: 0,
                        px: 1.5,
                        alignItems: 'center',
                        '& .MuiAlert-message': { width: '100%' }
                    }}
                    action={
                        <>
                            <IconButton
                                aria-label={expandedId === aviso.id ? t('page.avisos.collapse') : t('page.avisos.expand')}
                                aria-expanded={expandedId === aviso.id}
                                size="small"
                                onClick={() => handleToggleExpand(aviso.id)}
                            >
                                <Icon sx={{ fontSize: 20 }}>
                                    {expandedId === aviso.id ? 'expand_less' : 'expand_more'}
                                </Icon>
                            </IconButton>
                            <IconButton
                                aria-label={t('page.avisos.tanca')}
                                size="small"
                                onClick={() => minimitza(aviso.id)}
                            >
                                <Icon sx={{ fontSize: 20 }}>close</Icon>
                            </IconButton>
                        </>
                    }
                >
                    <AlertTitle sx={{ fontWeight: 'bold', m: 0, lineHeight: 1.4 }}>
                        {aviso.assumpte}
                    </AlertTitle>

                    <Collapse in={expandedId === aviso.id} timeout="auto" unmountOnExit>
                        <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>
                            {aviso.missatge}
                        </Typography>
                        {aviso.dataFinal && (
                            <Typography variant="caption" sx={{ display: 'block', mt: 1, opacity: 0.8 }}>
                                {t('page.avisos.validFinsA')}: {new Date(aviso.dataFinal).toLocaleDateString(currentLanguage)}
                            </Typography>
                        )}
                    </Collapse>
                </Alert>
            ))}
        </Box>
    );
};

export default AvisosAlert;
