import React from 'react';
import { useTranslation } from 'react-i18next';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import Divider from '@mui/material/Divider';
import Box from '@mui/material/Box';
import { MuiDialog, useCloseDialogButtons } from 'reactlib';
import { formatDate } from '../../util/dateUtils';
import { IntegracioEstat } from './IntegracioEstat';
import { IntegracioStacktrace } from './IntegracioStacktrace';

type DetailFieldSpec = {
    label: string;
    value?: React.ReactNode;
};

// eslint-disable-next-line react-refresh/only-export-components
const DetailRow: React.FC<{ fields: DetailFieldSpec[] }> = ({ fields }) => (
    <Stack direction="row" spacing={2} divider={<Divider orientation="vertical" flexItem />}>
        {fields.map((field, index) => (
            <Stack key={index} sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="subtitle2">{field.label}</Typography>
                <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                    {field.value}
                </Typography>
            </Stack>
        ))}
    </Stack>
);

/**
 * Diàleg amb el detall d'una petició d'integració. 
 * Mostra la fila de la graella tal com arriba, sense tornar-la a consultar.
 */
export const useIntegracioDetail = () => {
    const { t } = useTranslation();
    const closeButtons = useCloseDialogButtons();
    const [open, setOpen] = React.useState(false);
    const [row, setRow] = React.useState<any>();

    const handleOpen = (_id: any, r: any) => {
        setRow(r);
        setOpen(true);
    };

    const handleClose = (reason?: string) => {
        if (reason !== 'backdropClick') {
            setOpen(false);
        }
    };

    const parametres = Object.entries(row?.parametres ?? {});

    const dialog = (
        <MuiDialog
            open={open}
            closeCallback={handleClose}
            title={t('page.integracio.detail.title')}
            componentProps={{ fullWidth: true, maxWidth: 'md' }}
            buttons={closeButtons}
            buttonCallback={() => handleClose()}
        >
            <Stack spacing={2} divider={<Divider />} sx={{ pt: 1 }}>
                <DetailRow
                    fields={[
                        {
                            label: t('page.integracio.detail.data'),
                            value: row?.data ? formatDate(row.data, 'DD/MM/YYYY HH:mm:ss') : undefined,
                        },
                        {
                            label: t('page.integracio.detail.tipus'),
                            value: row?.tipus ? t('page.integracio.detail.tipusValor.' + row.tipus) : undefined,
                        },
                    ]}
                />
                <DetailRow
                    fields={[
                        {
                            label: t('page.integracio.detail.integracio'),
                            value: row?.codi ? t('page.integracio.pipella.' + row.codi) : undefined,
                        },
                        {
                            label: t('page.integracio.detail.endpoint'),
                            value: row?.endpoint,
                        },
                    ]}
                />
                <DetailRow
                    fields={[
                        {
                            label: t('page.integracio.detail.descripcio'),
                            value: row?.descripcio,
                        },
                    ]}
                />
                {parametres.length > 0 && (
                    <Stack>
                        <Typography variant="subtitle2">
                            {t('page.integracio.detail.parametres')}
                        </Typography>
                        {parametres.map(([clau, valor]) => (
                            <Typography key={clau} variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                                <strong>{clau}:</strong> {String(valor)}
                            </Typography>
                        ))}
                    </Stack>
                )}
                <DetailRow
                    fields={[
                        {
                            label: t('page.integracio.detail.tempsResposta'),
                            value: row?.tempsResposta != null ? `${row.tempsResposta} ms` : undefined,
                        },
                        {
                            label: t('page.integracio.detail.estat'),
                            value: row?.estat ? (
                                <Box sx={{ display: 'flex' }}>
                                    <IntegracioEstat value={row.estat} />
                                </Box>
                            ) : undefined,
                        },
                    ]}
                />
                {row?.estat === 'ERROR' && (
                    <>
                        <DetailRow
                            fields={[
                                {
                                    label: t('page.integracio.detail.errorDescripcio'),
                                    value: row?.errorDescripcio,
                                },
                            ]}
                        />
                        <DetailRow
                            fields={[
                                {
                                    label: t('page.integracio.detail.excepcioMessage'),
                                    value: row?.excepcioMessage,
                                },
                            ]}
                        />
                        {row?.excepcioStacktrace && (
                            <IntegracioStacktrace stacktrace={row.excepcioStacktrace} />
                        )}
                    </>
                )}
            </Stack>
        </MuiDialog>
    );

    return { show: handleOpen, close: handleClose, component: dialog };
};

export default useIntegracioDetail;
