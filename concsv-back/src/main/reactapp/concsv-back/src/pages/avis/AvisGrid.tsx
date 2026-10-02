import React from 'react';
import { useTranslation } from 'react-i18next';
import { GridPage, type MuiDataGridColDef, useMuiDataGridApiRef } from 'reactlib';
import AvisFormContent from './AvisFormContent';
import { useAvisAccions } from './AvisAccions';
import { CardPage } from '../../components/CardData';
import StyledMuiGrid from '../../components/StyledMuiGrid';
import { formatDate } from '../../util/dateUtils';

const columns: MuiDataGridColDef[] = [
    { field: 'assumpte', flex: 2 },
    { field: 'entitat', flex: 1.75 },
    {
        field: 'dataInici',
        flex: 0.5,
        valueFormatter: (value: string) => (value ? formatDate(value, 'DD/MM/YYYY') : ''),
    },
    {
        field: 'dataFinal',
        flex: 0.5,
        valueFormatter: (value: string) => (value ? formatDate(value, 'DD/MM/YYYY') : ''),
    },
    { field: 'actiu', flex: 0.35, type: 'boolean' },
    { field: 'avisNivell', flex: 0.5 },
];

const sortModel = [{ field: 'dataInici', sort: 'asc' as const }];

/**
 * Manteniment d'avisos: La creació i la modificació es fan en un formulari modal;
 * activar, desactivar i esborrar també es poden fer en bloc sobre les files seleccionades.
 */
export const AvisGrid: React.FC = () => {
    const { t } = useTranslation();
    const apiRef = useMuiDataGridApiRef();
    const refresh = () => apiRef.current?.refresh?.();
    const { accions, accionsMassives } = useAvisAccions(refresh);

    return (
        <GridPage>
            <CardPage title={t('page.avisos.grid.title')}>
                <StyledMuiGrid
                    toolbarCreateTitle={t('page.avisos.accio.nova')}
                    resourceName="avisResource"
                    apiRef={apiRef}
                    columns={columns}
                    sortModel={sortModel}
                    toolbarShowQuickFilter
                    paginationActive
                    popupEditActive
                    popupEditFormContent={<AvisFormContent />}
                    popupEditFormDialogResourceTitle={t('page.avisos.form.resourceTitle')}
                    popupEditFormI18nKeys={{
                        createSuccess: 'page.avisos.accio.crearOk',
                        updateSuccess: 'page.avisos.accio.modificarOk',
                        deleteSuccess: 'page.avisos.accio.esborrarOk',
                    }}
                    selectionActive
                    rowHideUpdateButton
                    rowHideDeleteButton
                    rowAdditionalActions={accions}
                    toolbarMassiveActions={accionsMassives}
                />
            </CardPage>
        </GridPage>
    );
};

export default AvisGrid;
