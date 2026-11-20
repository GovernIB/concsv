import React from 'react';
import Grid from '@mui/material/Grid';
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

/** Resultat de l'acció IMPORTAR (DocumentExclosResource.ResultatImportacio). */
type ResultatImportacio = { afegits?: number; jaExistents?: number; descartats?: number };
/** Resultat de l'acció ACTUALITZAR (DocumentExclosResource.ResultatActualitzacio). */
type ResultatActualitzacio = { actualitzats?: number; noTrobats?: number; errors?: number };
/** Resultat de l'acció ACTUALITZAR_MASSIU (DocumentExclosResource.ResultatActualitzacioMassiva). */
type ResultatActualitzacioMassiva = { execucioId?: number | null; total?: number; omesos?: number };
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

    /**
     * Crea una execució massiva amb tota la selecció: el servidor ho fa en segon pla i la petició torna a l'acte, de
     * manera que el missatge només avisa que s'ha posat en marxa (info, no èxit: encara no s'ha actualitzat res).
     * El seguiment és a la pantalla d'execucions massives.
     */
    const programaActualitzacio = (ids: unknown[]) => {
        apiArtifactAction(undefined, { code: ACCIO_ACTUALITZAR_MASSIU, data: { ids } })
            .then((resultat: unknown) => {
                const { execucioId, total = 0, omesos = 0 } = (resultat ?? {}) as ResultatActualitzacioMassiva;
                temporalMessageShow(
                    null,
                    execucioId
                        ? t(
                              omesos > 0
                                  ? 'page.documentsExclosos.accio.actualitzaMassivaSegonPlaOmesos'
                                  : 'page.documentsExclosos.accio.actualitzaMassivaSegonPla',
                              { total, omesos }
                          )
                        : t('page.documentsExclosos.accio.actualitzaMassivaSenseElements', { omesos }),
                    execucioId ? 'info' : 'warning'
                );
            })
            .catch(mostraError);
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
                programaActualitzacio(ids);
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

    return { accions, accionsMassives, botonsToolbar, dialegImportacio };
};

export default useDocumentExclosAccions;
