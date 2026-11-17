import React from 'react';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Grid from '@mui/material/Grid';
import LinearProgress from '@mui/material/LinearProgress';
import Typography from '@mui/material/Typography';
import { useTranslation } from 'react-i18next';
import { useBaseAppContext, useConfirmDialogButtons, useResourceApiService, type MuiDataGridProps } from 'reactlib';
import FormActionDialog, { type FormActionDialogApi } from '../../components/FormActionDialog';
import GridFormField from '../../components/GridFormField';
import type { MassiveActionProps } from '../../components/MassiveActionSelector';
import { ToolbarButton } from '../../components/StyledMuiGrid';
import { icons } from '../../util/icons';

const RESOURCE_NAME = 'documentExclosResource';
const INFORME_EXPORTAR = 'EXPORTAR';
const ACCIO_IMPORTAR = 'IMPORTAR';
const ACCIO_ELIMINAR_MASSIU = 'ELIMINAR_MASSIU';
const ACCIO_ACTUALITZAR = 'ACTUALITZAR';
const ACCIO_ACTUALITZAR_MASSIU = 'ACTUALITZAR_MASSIU';
/**
 * Documents per petició de l'actualització massiva: el backend en rebutja més de 10 (cada un és una consulta a
 * l'Arxiu i la petició té temps màxim).
 */
const MIDA_LOT_ACTUALITZACIO = 10;

/** Resultat de l'acció IMPORTAR (DocumentExclosResource.ResultatImportacio). */
type ResultatImportacio = { afegits?: number; jaExistents?: number; descartats?: number };
/** Resultat de les accions ACTUALITZAR i ACTUALITZAR_MASSIU (DocumentExclosResource.ResultatActualitzacio). */
type ResultatActualitzacio = { actualitzats?: number; noTrobats?: number; errors?: number; pendents?: unknown[] };
type Progres = { fets: number; total: number };
type ErrorApi = { description?: string; message?: string };

type AccionsFila = NonNullable<MuiDataGridProps['rowAdditionalActions']>;
type ElementsToolbar = NonNullable<MuiDataGridProps['toolbarElementsWithPositions']>;

export const useDocumentExclosAccions = (
    refresh: () => void
): {
    accions: AccionsFila;
    accionsMassives: MassiveActionProps[];
    botonsToolbar: ElementsToolbar;
    dialegImportacio: React.ReactElement;
    dialegProgres: React.ReactElement;
} => {
    const { t } = useTranslation();
    const { temporalMessageShow, messageDialogShow, saveAs } = useBaseAppContext();
    const confirmDialogButtons = useConfirmDialogButtons();
    const {
        isReady: apiIsReady,
        artifactReport: apiArtifactReport,
        artifactAction: apiArtifactAction,
    } = useResourceApiService(RESOURCE_NAME);
    const importacioApiRef = React.useRef<FormActionDialogApi | undefined>(undefined);
    // El progrés es manté en tancar el diàleg, perquè durant l'animació de sortida no es vegi "0 de 0".
    const [progres, setProgres] = React.useState<Progres>({ fets: 0, total: 0 });
    const [progresObert, setProgresObert] = React.useState(false);
    const actualitzacioCancelada = React.useRef(false);

    const mostraError = (error: ErrorApi) =>
        temporalMessageShow(
            t('page.documentsExclosos.accio.error'),
            error?.description ?? error?.message ?? '',
            'error'
        );

    // --- Actualització de les dades del document ---------------------------------------------
    const actualitzaFila = (id: unknown) => {
        if (!apiIsReady) {
            return;
        }
        apiArtifactAction(id, { code: ACCIO_ACTUALITZAR })
            .then((resultat: unknown) => {
                refresh();
                const { noTrobats = 0, errors = 0 } = (resultat ?? {}) as ResultatActualitzacio;
                if (errors > 0) {
                    temporalMessageShow(null, t('page.documentsExclosos.accio.actualitzaFilaError'), 'error');
                } else if (noTrobats > 0) {
                    temporalMessageShow(null, t('page.documentsExclosos.accio.actualitzaFilaNoTrobat'), 'warning');
                } else {
                    temporalMessageShow(null, t('page.documentsExclosos.accio.actualitzaFilaOk'), 'success');
                }
            })
            .catch(mostraError);
    };

    /** Envia la selecció en lots, perquè cap petició superi el temps màxim; es pot cancel·lar entre lots. */
    const actualitzaLots = async (ids: unknown[]) => {
        const totals = { actualitzats: 0, noTrobats: 0, errors: 0 };
        let pendents = [...ids];
        let fets = 0;
        actualitzacioCancelada.current = false;
        setProgres({ fets, total: ids.length });
        setProgresObert(true);
        try {
            while (pendents.length > 0 && !actualitzacioCancelada.current) {
                const lot = pendents.slice(0, MIDA_LOT_ACTUALITZACIO);
                const resta = pendents.slice(MIDA_LOT_ACTUALITZACIO);
                const resultat = (await apiArtifactAction(undefined, {
                    code: ACCIO_ACTUALITZAR_MASSIU,
                    data: { ids: lot },
                })) as ResultatActualitzacio;
                totals.actualitzats += resultat?.actualitzats ?? 0;
                totals.noTrobats += resultat?.noTrobats ?? 0;
                totals.errors += resultat?.errors ?? 0;
                // Els que el servidor no ha tingut temps de processar tornen a la cua.
                const noProcessats = resultat?.pendents ?? [];
                fets += lot.length - noProcessats.length;
                setProgres({ fets, total: ids.length });
                if (noProcessats.length >= lot.length) {
                    // Sense progrés: es para, per no repetir la mateixa petició per sempre.
                    totals.errors += noProcessats.length + resta.length;
                    pendents = [];
                } else {
                    pendents = [...noProcessats, ...resta];
                }
            }
        } catch (error) {
            mostraError(error as ErrorApi);
        } finally {
            setProgresObert(false);
            refresh();
        }
        const cancelada = actualitzacioCancelada.current && pendents.length > 0;
        temporalMessageShow(
            null,
            t(
                cancelada
                    ? 'page.documentsExclosos.accio.actualitzaMassivaCancelada'
                    : 'page.documentsExclosos.accio.actualitzaMassivaOk',
                { ...totals, pendents: pendents.length }
            ),
            totals.errors > 0 || cancelada ? 'warning' : 'success'
        );
    };

    const actualitzaSeleccio = (ids: unknown[]) => {
        if (!apiIsReady || !ids?.length) {
            return;
        }
        messageDialogShow(
            t('page.documentsExclosos.accio.actualitza'),
            t('page.documentsExclosos.accio.confirmarActualitzacio', { count: ids.length }),
            confirmDialogButtons,
            { maxWidth: 'sm', fullWidth: true }
        ).then((confirmat: unknown) => {
            if (confirmat) {
                actualitzaLots(ids);
            }
        });
    };

    // --- Accions de fila ---------------------------------------------------------------------
    const accions: AccionsFila = [
        {
            label: t('page.documentsExclosos.accio.modificar'),
            icon: icons.modifica,
            showInMenu: true,
            rowLink: 'update',
            clickShowUpdateDialog: true,
        },
        {
            label: t('page.documentsExclosos.accio.actualitza'),
            icon: icons.sincronitza,
            showInMenu: true,
            action: ACCIO_ACTUALITZAR,
            onClick: (id: unknown) => actualitzaFila(id),
        },
        {
            label: t('page.documentsExclosos.accio.esborrar'),
            icon: icons.esborra,
            showInMenu: true,
            rowLink: 'delete',
            clickTriggerDelete: true,
        },
    ];

    // --- Accions massives --------------------------------------------------------------------
    const eliminaSeleccio = (ids: unknown[]) => {
        if (!apiIsReady || !ids?.length) {
            return;
        }
        messageDialogShow(
            t('page.documentsExclosos.accio.esborrar'),
            t('page.documentsExclosos.accio.confirmarEsborrat', { count: ids.length }),
            confirmDialogButtons,
            { maxWidth: 'sm', fullWidth: true }
        ).then((confirmat: unknown) => {
            if (!confirmat) {
                return;
            }
            apiArtifactAction(undefined, { code: ACCIO_ELIMINAR_MASSIU, data: { ids } })
                .then(() => {
                    refresh();
                    temporalMessageShow(
                        null,
                        t('page.documentsExclosos.accio.massivaEsborrarOk', { count: ids.length }),
                        'success'
                    );
                })
                .catch(mostraError);
        });
    };

    const accionsMassives: MassiveActionProps[] = [
        {
            label: t('page.documentsExclosos.accio.actualitza'),
            icon: icons.sincronitza,
            showInMenu: true,
            onClick: actualitzaSeleccio,
        },
        {
            label: t('page.documentsExclosos.accio.esborrar'),
            icon: icons.esborra,
            showInMenu: true,
            onClick: eliminaSeleccio,
        },
    ];

    // --- Barra d'eines: descàrrega i càrrega del fitxer --------------------------------------
    const descarrega = () => {
        if (!apiIsReady) {
            return;
        }
        // 'CUSTOM' no és als tipus de fitxer de la llibreria (només els de Jasper), però el
        // backend l'accepta: el fitxer el genera el propi servei.
        apiArtifactReport(undefined, { code: INFORME_EXPORTAR, fileType: 'CUSTOM' as never })
            .then((resposta) => {
                const { blob, fileName } = resposta as { blob: Blob; fileName?: string };
                saveAs?.(blob, fileName);
            })
            .catch(mostraError);
    };

    const resumImportacio = (resultat?: ResultatImportacio) => {
        refresh();
        temporalMessageShow(
            null,
            t('page.documentsExclosos.accio.importacioOk', {
                afegits: resultat?.afegits ?? 0,
                jaExistents: resultat?.jaExistents ?? 0,
                descartats: resultat?.descartats ?? 0,
            }),
            (resultat?.descartats ?? 0) > 0 ? 'warning' : 'success'
        );
    };

    const botonsToolbar: ElementsToolbar = [
        {
            position: 3,
            element: (
                <ToolbarButton
                    title={t('page.documentsExclosos.accio.descarrega')}
                    icon={icons.descarrega}
                    variant="contained"
                    onClick={descarrega}
                >
                    {t('page.documentsExclosos.accio.descarrega')}
                </ToolbarButton>
            ),
        },
        {
            position: 3,
            element: (
                <ToolbarButton
                    title={t('page.documentsExclosos.accio.carrega')}
                    icon={icons.carrega}
                    variant="contained"
                    onClick={() => importacioApiRef.current?.show(undefined)}
                >
                    {t('page.documentsExclosos.accio.carrega')}
                </ToolbarButton>
            ),
        },
    ];

    const dialegImportacio = (
        <FormActionDialog
            resourceName={RESOURCE_NAME}
            action={ACCIO_IMPORTAR}
            title={t('page.documentsExclosos.accio.carrega')}
            apiRef={importacioApiRef}
            buttons={[
                {
                    icon: icons.carrega,
                    text: t('page.documentsExclosos.accio.carrega'),
                    componentProps: { variant: 'contained' },
                    value: true,
                },
                {
                    text: t('common.cancel'),
                    componentProps: { variant: 'outlined' },
                    value: false,
                },
            ]}
            dialogComponentProps={{ fullWidth: true, maxWidth: 'sm' }}
            onSuccess={resumImportacio}
        >
            <Grid container spacing={2}>
                <GridFormField size={12} name="fitxer" accept=".txt,text/plain" />
            </Grid>
        </FormActionDialog>
    );

    const dialegProgres = (
        <Dialog open={progresObert} fullWidth maxWidth="sm">
            <DialogTitle>{t('page.documentsExclosos.accio.actualitza')}</DialogTitle>
            <DialogContent>
                <Typography sx={{ mb: 2 }}>
                    {t('page.documentsExclosos.accio.actualitzaProgres', {
                        fets: progres.fets,
                        total: progres.total,
                    })}
                </Typography>
                <LinearProgress
                    variant="determinate"
                    value={progres.total > 0 ? (progres.fets / progres.total) * 100 : 0}
                />
            </DialogContent>
            <DialogActions>
                <Button variant="outlined" onClick={() => (actualitzacioCancelada.current = true)}>
                    {t('common.cancel')}
                </Button>
            </DialogActions>
        </Dialog>
    );

    return { accions, accionsMassives, botonsToolbar, dialegImportacio, dialegProgres };
};

export default useDocumentExclosAccions;
