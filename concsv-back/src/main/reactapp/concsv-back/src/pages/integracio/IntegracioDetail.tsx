import React from 'react';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import { MuiDialog, useCloseDialogButtons } from 'reactlib';
import { DetailCard, DetailCardContent } from '../../components/CardData';
import { formatDate } from '../../util/dateUtils';
import { IntegracioEstat } from './IntegracioEstat';
import { IntegracioStacktrace } from './IntegracioStacktrace';

/** Propietats comunes dels camps: etiqueta amb amplada fixa perquè els valors quedin alineats. */
const INLINE = { titleSize: 'auto', textSize: 'grow', labelWidth: 140 } as const;

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
    const isError = row?.estat === 'ERROR';

    const dialog = (
        <MuiDialog
            open={open}
            closeCallback={handleClose}
            title={t('page.integracio.detail.title')}
            componentProps={{ fullWidth: true, maxWidth: 'lg' }}
            buttons={closeButtons}
            buttonCallback={() => handleClose()}
        >
            <Grid container>
                <DetailCard cardProps={{ mb: 2 }}>
                    <DetailCardContent title={t('page.integracio.detail.data')} size={6} {...INLINE}>
                        {row?.data ? formatDate(row.data, 'DD/MM/YYYY HH:mm:ss') : undefined}
                    </DetailCardContent>
                    <DetailCardContent title={t('page.integracio.detail.tipus')} size={6} {...INLINE}>
                        {row?.tipus ? t('page.integracio.detail.tipusValor.' + row.tipus) : undefined}
                    </DetailCardContent>
                    <DetailCardContent title={t('page.integracio.detail.integracio')} size={6} {...INLINE}>
                        {row?.codi ? t('page.integracio.pipella.' + row.codi) : undefined}
                    </DetailCardContent>
                    <DetailCardContent title={t('page.integracio.detail.endpoint')} size={6} {...INLINE}>
                        {row?.endpoint}
                    </DetailCardContent>
                    <DetailCardContent title={t('page.integracio.detail.descripcio')} {...INLINE}>
                        {row?.descripcio}
                    </DetailCardContent>
                    <DetailCardContent title={t('page.integracio.detail.parametres')} {...INLINE} isObject hiddenIfEmpty>
                        {parametres.length > 0 && (
                            <Box component="ul" sx={{ m: 0, pl: 2 }}>
                                {parametres.map(([clau, valor]) => (
                                    <li key={clau}>
                                        <strong>{clau}:</strong> {String(valor)}
                                    </li>
                                ))}
                            </Box>
                        )}
                    </DetailCardContent>
                    <DetailCardContent title={t('page.integracio.detail.tempsResposta')} size={6} {...INLINE}>
                        {row?.tempsResposta != null ? `${row.tempsResposta} ms` : undefined}
                    </DetailCardContent>
                    <DetailCardContent title={t('page.integracio.detail.estat')} size={6} {...INLINE} isObject>
                        {row?.estat && (
                            <Box sx={{ display: 'flex' }}>
                                <IntegracioEstat value={row.estat} />
                            </Box>
                        )}
                    </DetailCardContent>
                    {isError && (
                        <>
                            <DetailCardContent title={t('page.integracio.detail.errorDescripcio')} {...INLINE} hiddenIfEmpty>
                                {row?.errorDescripcio}
                            </DetailCardContent>
                            <DetailCardContent title={t('page.integracio.detail.excepcioMessage')} {...INLINE} hiddenIfEmpty>
                                {row?.excepcioMessage}
                            </DetailCardContent>
                        </>
                    )}
                </DetailCard>
                {isError && row?.excepcioStacktrace && (
                    <Grid size={12}>
                        <IntegracioStacktrace stacktrace={row.excepcioStacktrace} />
                    </Grid>
                )}
            </Grid>
        </MuiDialog>
    );

    return { show: handleOpen, close: handleClose, component: dialog };
};

export default useIntegracioDetail;
