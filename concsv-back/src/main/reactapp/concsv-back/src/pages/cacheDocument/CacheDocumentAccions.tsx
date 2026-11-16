import { useTranslation } from 'react-i18next';
import { useBaseAppContext, useConfirmDialogButtons, useResourceApiService } from 'reactlib';
import type { MassiveActionProps } from '../../components/MassiveActionSelector';
import { missatgeError } from '../propietats/PropietatsUtils';
import { icons } from '../../util/icons';

const RESOURCE_NAME = 'cacheDocumentResource';
const ACCIO_ESBORRAR_MASSIU = 'ESBORRAR_MASSIU';

/** Resultat de l'acció ESBORRAR_MASSIU (CacheDocumentResource.ResultatEsborrat). */
type ResultatEsborrat = { esborrats?: number; errors?: number };

/**
 * Accions massives del llistat de la cache de documents: esborrar les entrades seleccionades, amb
 * confirmació. Amb el filtre del llistat i "Selecciona-ho tot" fa el mateix que feien els botons d'esborrar
 * una carpeta o tota la cache.
 */
export const useCacheDocumentAccions = (refresh: () => void): { accionsMassives: MassiveActionProps[] } => {
    const { t } = useTranslation();
    const { temporalMessageShow, messageDialogShow } = useBaseAppContext();
    const confirmDialogButtons = useConfirmDialogButtons();
    const { isReady, artifactAction } = useResourceApiService(RESOURCE_NAME);

    const esborraSeleccio = (ids: unknown[]) => {
        if (!isReady || !ids?.length) {
            return;
        }
        messageDialogShow(
            t('page.cacheDocuments.accio.esborrar'),
            t('page.cacheDocuments.accio.confirmarEsborrat', { count: ids.length }),
            confirmDialogButtons,
            { maxWidth: 'sm', fullWidth: true }
        ).then((confirmat: unknown) => {
            if (!confirmat) {
                return;
            }
            artifactAction(undefined, { code: ACCIO_ESBORRAR_MASSIU, data: { ids } })
                .then((resultat: ResultatEsborrat) => {
                    refresh();
                    if (resultat?.errors) {
                        temporalMessageShow(
                            null,
                            t('page.cacheDocuments.accio.okAmbErrors', {
                                esborrats: resultat.esborrats ?? 0,
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
                })
                .catch((error: unknown) => {
                    refresh();
                    temporalMessageShow(t('page.cacheDocuments.accio.error'), missatgeError(error), 'error');
                });
        });
    };

    const accionsMassives: MassiveActionProps[] = [
        {
            label: t('page.cacheDocuments.accio.esborrar'),
            icon: icons.esborra,
            showInMenu: true,
            onClick: esborraSeleccio,
        },
    ];

    return { accionsMassives };
};

export default useCacheDocumentAccions;
