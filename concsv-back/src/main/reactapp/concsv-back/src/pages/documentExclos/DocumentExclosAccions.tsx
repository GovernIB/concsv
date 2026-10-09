import React from 'react';
import Grid from '@mui/material/Grid';
import { useTranslation } from 'react-i18next';
import { useBaseAppContext, useConfirmDialogButtons, useResourceApiService, type MuiDataGridProps } from 'reactlib';
import FormActionDialog, { type FormActionDialogApi } from '../../components/FormActionDialog';
import GridFormField from '../../components/GridFormField';
import type { MassiveActionProps } from '../../components/MassiveActionSelector';
import { ToolbarButton } from '../../components/StyledMuiGrid';

const RESOURCE_NAME = 'documentExclosResource';
const INFORME_EXPORTAR = 'EXPORTAR';
const ACCIO_IMPORTAR = 'IMPORTAR';
const ACCIO_ELIMINAR_MASSIU = 'ELIMINAR_MASSIU';

/** Resultat de l'acció IMPORTAR (DocumentExclosResource.ResultatImportacio). */
type ResultatImportacio = { afegits?: number; jaExistents?: number; descartats?: number };
type ErrorApi = { description?: string; message?: string };

type AccionsFila = NonNullable<MuiDataGridProps['rowAdditionalActions']>;
type ElementsToolbar = NonNullable<MuiDataGridProps['toolbarElementsWithPositions']>;

/**
 * Accions del llistat de documents exclosos:
 *  - de fila: modificar i esborrar;
 *  - massives (files seleccionades): esborrar en bloc, amb confirmació;
 *  - de barra d'eines: descarregar i carregar el fitxer `documents-exclosos.txt`.
 */
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

    // --- Accions de fila ---------------------------------------------------------------------
    const accions: AccionsFila = [
        {
            label: t('page.documentsExclosos.accio.modificar'),
            icon: 'edit',
            showInMenu: true,
            rowLink: 'update',
            clickShowUpdateDialog: true,
        },
        {
            label: t('page.documentsExclosos.accio.esborrar'),
            icon: 'delete',
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
            label: t('page.documentsExclosos.accio.esborrar'),
            icon: 'delete',
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
                    icon="file_download"
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
                    icon="file_upload"
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
                    icon: 'file_upload',
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
