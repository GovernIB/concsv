import React, { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Button, Grid, Icon, Typography } from '@mui/material';
import {
    MuiActionReportButton as ActionReportButton,
    GridPage,
    useBaseAppContext,
    useResourceApiService,
} from 'reactlib';
import { CardPage } from '../../components/CardData';
import { missatgeError } from '../propietats/PropietatsUtils';

const ACCIO_COMPTAR = 'COMPTAR_CACHE';

type Carpeta = 'IMPRIMIBLE' | 'INFO' | 'ORIGINAL';

/** Nombre de fitxers de cada carpeta i carpeta consultada (ConfigResource.ResumCache). */
type ResumCache = { info: number; original: number; imprimible: number; ruta: string };

type ResultatEsborrat = { esborrats: number; errors: number };

const BOTONS_CARPETA: { carpeta: Carpeta; accio: string }[] = [
    { carpeta: 'IMPRIMIBLE', accio: 'ESBORRAR_CACHE_IMPRIMIBLE' },
    { carpeta: 'INFO', accio: 'ESBORRAR_CACHE_INFO' },
    { carpeta: 'ORIGINAL', accio: 'ESBORRAR_CACHE_ORIGINAL' },
];
const ACCIO_ESBORRAR_TOTES = 'ESBORRAR_CACHE_TOTES';

const recompteCarpeta = (resum: ResumCache, carpeta: Carpeta) =>
    ({ IMPRIMIBLE: resum.imprimible, INFO: resum.info, ORIGINAL: resum.original })[carpeta];

/** Ruta de la carpeta, amb el separador que usa la ruta de la cache del servidor. */
const rutaCarpeta = (ruta: string, carpeta?: Carpeta) =>
    carpeta ? ruta + (ruta.includes('\\') ? '\\' : '/') + carpeta : ruta;

/**
 * Contingut del diàleg de confirmació. Consulta el nombre de fitxers (acció COMPTAR_CACHE) quan
 * s'obre el diàleg, de manera que sempre és el de just abans de confirmar.
 */
const ConfirmacioEsborrat: React.FC<{ carpeta?: Carpeta }> = ({ carpeta }) => {
    const { t } = useTranslation();
    const { isReady, artifactAction } = useResourceApiService('configResource');
    const [resum, setResum] = useState<ResumCache>();
    const [error, setError] = useState(false);

    useEffect(() => {
        if (isReady) {
            artifactAction(undefined, { code: ACCIO_COMPTAR })
                .then((resultat: ResumCache) => setResum(resultat))
                .catch(() => setError(true));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isReady]);

    const recompte = () => {
        if (error) {
            return t('page.cacheDocuments.accio.recompteError');
        }
        if (!resum) {
            return t('page.cacheDocuments.accio.recompteCarregant');
        }
        if (carpeta) {
            return t('page.cacheDocuments.accio.confirmRecompte', { count: recompteCarpeta(resum, carpeta) });
        }
        return t('page.cacheDocuments.accio.confirmRecompteTotes', {
            count: resum.info + resum.original + resum.imprimible,
            info: resum.info,
            original: resum.original,
            imprimible: resum.imprimible,
        });
    };

    return (
        <>
            <Typography>
                {carpeta
                    ? t('page.cacheDocuments.accio.confirmCarpeta', { carpeta })
                    : t('page.cacheDocuments.accio.confirmTotes')}
            </Typography>
            <Typography sx={{ mt: 1, fontWeight: 'bold' }}>{recompte()}</Typography>
            {resum?.ruta && (
                <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 1 }}>
                    {t('page.cacheDocuments.accio.ruta', { ruta: rutaCarpeta(resum.ruta, carpeta) })}
                </Typography>
            )}
        </>
    );
};

/**
 * Manteniment de la cache de documents.
 * Cada botó és una acció de ConfigResource que esborra els fitxers d'una carpeta de la cache, o de totes.
 */
export const CacheDocument: React.FC = () => {
    const { t } = useTranslation();
    const { temporalMessageShow } = useBaseAppContext();
    const { isReady, artifactAction } = useResourceApiService('configResource');
    const [resum, setResum] = useState<ResumCache>();

    const [carregant, setCarregant] = useState(false);

    const refrescarBotons = useCallback(() => {
        if (isReady) {
            setCarregant(true);
            artifactAction(undefined, { code: ACCIO_COMPTAR })
                .then((resultat: ResumCache) => setResum(resultat))
                // Sense recompte els botons es mostren sense el nombre
                .catch(() => setResum(undefined))
                .finally(() => setCarregant(false));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isReady]);

    useEffect(refrescarBotons, [refrescarBotons]);

    const onSucces = (resultat?: ResultatEsborrat) => {
        refrescarBotons();
        if (resultat && resultat.errors > 0) {
            temporalMessageShow(
                null,
                t('page.cacheDocuments.accio.okAmbErrors', {
                    esborrats: resultat.esborrats,
                    errors: resultat.errors,
                }),
                'warning'
            );
        } else {
            temporalMessageShow(
                null,
                t('page.cacheDocuments.accio.ok', { esborrats: resultat?.esborrats ?? 0 }),
                'success'
            );
        }
    };

    const onError = (error: unknown) => {
        refrescarBotons();
        temporalMessageShow(t('page.cacheDocuments.accio.error'), missatgeError(error), 'error');
    };

    const titolCarpeta = (carpeta: Carpeta) =>
        t('page.cacheDocuments.accio.esborrarCarpeta', { carpeta }) +
        (resum ? ` (${recompteCarpeta(resum, carpeta)})` : '');

    const titolTotes = () =>
        t('page.cacheDocuments.accio.esborrarTotes') +
        (resum ? ` (${resum.info + resum.original + resum.imprimible})` : '');

    return (
        <GridPage>
            <CardPage title={t('page.cacheDocuments.title')}>
                <Grid container spacing={2}>
                    <Grid size={12} sx={{ px: 1 }}>
                        <Typography color="text.secondary">{t('page.cacheDocuments.descripcio.intro')}</Typography>
                        <Box component="ul" sx={{ color: 'text.secondary', my: 1, pl: 3 }}>
                            {BOTONS_CARPETA.map(({ carpeta }) => (
                                <li key={carpeta}>
                                    <Typography component="span" color="text.primary" sx={{ fontWeight: 'bold' }}>
                                        {carpeta}
                                    </Typography>
                                    {': ' + t(`page.cacheDocuments.descripcio.${carpeta.toLowerCase()}`)}
                                </li>
                            ))}
                        </Box>
                        <Typography color="text.secondary">{t('page.cacheDocuments.descripcio.final')}</Typography>
                    </Grid>
                    <Grid size={12} sx={{ my: 1 }} display={'flex'} flexWrap={'wrap'} gap={1}>
                        <Button
                            variant="outlined"
                            size="small"
                            startIcon={<Icon>refresh</Icon>}
                            disabled={!isReady || carregant}
                            onClick={refrescarBotons}
                            sx={{ borderRadius: '4px', ml: 'auto' }}
                        >
                            {t('page.cacheDocuments.accio.refrescar')}
                        </Button>
                        {BOTONS_CARPETA.map(({ carpeta, accio }) => (
                            <ActionReportButton
                                key={accio}
                                resourceName="configResource"
                                action={accio}
                                title={titolCarpeta(carpeta)}
                                buttonIcon="delete"
                                confirm
                                confirmMessage={<ConfirmacioEsborrat carpeta={carpeta} />}
                                onSuccess={onSucces}
                                onError={onError}
                                buttonComponentProps={{
                                    variant: 'contained',
                                    size: 'small',
                                    sx: { borderRadius: '4px', '& .MuiButton-startIcon': { mr: 1 } },
                                }}
                            />
                        ))}
                        <ActionReportButton
                            resourceName="configResource"
                            action={ACCIO_ESBORRAR_TOTES}
                            title={titolTotes()}
                            buttonIcon="delete_sweep"
                            confirm
                            confirmMessage={<ConfirmacioEsborrat />}
                            onSuccess={onSucces}
                            onError={onError}
                            buttonComponentProps={{
                                variant: 'contained',
                                color: 'error',
                                size: 'small',
                                sx: { borderRadius: '4px', '& .MuiButton-startIcon': { mr: 1.5 } },
                            }}
                        />
                    </Grid>
                </Grid>
            </CardPage>
        </GridPage>
    );
};

export default CacheDocument;
