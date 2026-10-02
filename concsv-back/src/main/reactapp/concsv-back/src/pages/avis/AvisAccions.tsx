import { useTranslation } from 'react-i18next';
import { useBaseAppContext, useResourceApiService, type MuiDataGridProps } from 'reactlib';
import type { MassiveActionProps } from '../../components/MassiveActionSelector';

const ACCIO_ACTIVAR = 'ACTIVAR';
const ACCIO_DESACTIVAR = 'DESACTIVAR';
const ACCIO_MASSIVA = 'ACCIO_MASSIVA';

type AccioMassiva = 'activar' | 'desactivar' | 'eliminar';

type AccionsFila = NonNullable<MuiDataGridProps['rowAdditionalActions']>;

/**
 * Accions del menú de cada fila del llistat d'avisos i accions massives de la barra d'eines.
 *
 * Les accions massives criden l'acció ACCIO_MASSIVA (sense id) amb la llista d'identificadors
 * seleccionats.
 */
export const useAvisAccions = (
    refresh: () => void
): { accions: AccionsFila; accionsMassives: MassiveActionProps[] } => {
    const { t } = useTranslation();
    const { temporalMessageShow } = useBaseAppContext();
    const { isReady: apiIsReady, artifactAction: apiArtifactAction } =
        useResourceApiService('avisResource');

    const executar = (promesa: Promise<any>, clauMissatgeOk: string) =>
        promesa
            .then(() => {
                refresh();
                temporalMessageShow(null, t(clauMissatgeOk), 'success');
            })
            .catch((error: any) =>
                temporalMessageShow(
                    t('page.avisos.accio.error'),
                    error?.description ?? error?.message,
                    'error'
                )
            );

    const executarAccio = (id: any, code: string, clauMissatgeOk: string) => {
        if (apiIsReady) {
            executar(apiArtifactAction(id, { code }), clauMissatgeOk);
        }
    };

    const executarMassiva = (ids: any[], accio: AccioMassiva, clauMissatgeOk: string) => {
        if (apiIsReady && ids?.length) {
            executar(
                apiArtifactAction(undefined, { code: ACCIO_MASSIVA, data: { accio, ids } }),
                clauMissatgeOk
            );
        }
    };

    const accions: AccionsFila = [
        {
            label: t('page.avisos.accio.modificar'),
            icon: 'edit',
            showInMenu: true,
            rowLink: 'update',
            clickShowUpdateDialog: true,
        },
        {
            label: t('page.avisos.accio.activar'),
            icon: 'check',
            showInMenu: true,
            action: ACCIO_ACTIVAR,
            hidden: (row: any) => row?.actiu,
            onClick: (id: any) => executarAccio(id, ACCIO_ACTIVAR, 'page.avisos.accio.activarOk'),
        },
        {
            label: t('page.avisos.accio.desactivar'),
            icon: 'close',
            showInMenu: true,
            action: ACCIO_DESACTIVAR,
            hidden: (row: any) => !row?.actiu,
            onClick: (id: any) =>
                executarAccio(id, ACCIO_DESACTIVAR, 'page.avisos.accio.desactivarOk'),
        },
        {
            label: t('page.avisos.accio.esborrar'),
            icon: 'delete',
            showInMenu: true,
            rowLink: 'delete',
            clickTriggerDelete: true,
        },
    ];

    const accionsMassives: MassiveActionProps[] = [
        {
            label: t('page.avisos.accio.activar'),
            icon: 'check',
            showInMenu: true,
            onClick: (ids: any[]) =>
                executarMassiva(ids, 'activar', 'page.avisos.accio.massivaActivarOk'),
        },
        {
            label: t('page.avisos.accio.desactivar'),
            icon: 'close',
            showInMenu: true,
            onClick: (ids: any[]) =>
                executarMassiva(ids, 'desactivar', 'page.avisos.accio.massivaDesactivarOk'),
        },
        {
            label: t('page.avisos.accio.esborrar'),
            icon: 'delete',
            showInMenu: true,
            onClick: (ids: any[]) =>
                executarMassiva(ids, 'eliminar', 'page.avisos.accio.massivaEsborrarOk'),
        },
    ];

    return { accions, accionsMassives };
};

export default useAvisAccions;
