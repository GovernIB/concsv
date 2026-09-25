import React from 'react';
import { useTranslation } from 'react-i18next';
import { GridPage, type MuiDataGridColDef, useMuiDataGridApiRef } from 'reactlib';
import EntitatFilter from './EntitatFilter';
import EntitatFormContent from './EntitatFormContent';
import { useEntitatAccions } from './EntitatAccions';
import { CardPage } from '../../components/CardData';
import StyledMuiGrid from '../../components/StyledMuiGrid';

// Les capçaleres no es declaren aquí: el MuiDataGrid les omple amb l'etiqueta que el backend
// publica per a cada camp (el `_prompt` del HAL-FORMS, veure concsv-back-rest-messages). És la
// mateixa font que fan servir el formulari, el filtre i les capçaleres del fitxer d'exportació.
const columns: MuiDataGridColDef[] = [
    { field: 'codi', flex: 1 },
    { field: 'nom', flex: 3 },
    { field: 'descripcio', flex: 3 },
    { field: 'cif', flex: 1 },
    { field: 'codiDir3', flex: 1 },
    { field: 'activa', flex: 0.6, type: 'boolean' },
];

/**
 * Manteniment d'entitats (només superusuari, veure PANTALLA_ROLS). La creació i la modificació es
 * fan en un formulari modal; activar i desactivar són accions del recurs.
 */
export const EntitatGrid: React.FC = () => {
    const { t } = useTranslation();
    const apiRef = useMuiDataGridApiRef();
    // El filtre es manté en un estat explícit perquè la graella el rebi per la prop `filter` i el
    // xip de la barra d'eines pugui comptar-ne els criteris aplicats.
    const [springFilter, setSpringFilter] = React.useState<string>();
    // La creació, la modificació i l'esborrat ja refresquen la graella pel seu compte: el refresc
    // només cal per a les accions pròpies (activar/desactivar).
    const refresh = () => apiRef.current?.refresh?.();
    const accions = useEntitatAccions(refresh);
    return (
        <GridPage>
            <CardPage title={t('page.entitats.grid.title')}>
                <EntitatFilter onSpringFilterChange={setSpringFilter} />
                <StyledMuiGrid
                    toolbarCreateTitle={t('page.entitats.accio.nova')}
                    resourceName="entitatResource"
                    apiRef={apiRef}
                    columns={columns}
                    filter={springFilter}
                    toolbarShowFilterCount
                    paginationActive
                    // El botó de crear de la barra d'eines i l'acció "Modifica" obren el mateix
                    // formulari dins un diàleg.
                    popupEditActive
                    popupEditFormContent={<EntitatFormContent />}
                    popupEditFormDialogResourceTitle={t('page.entitats.form.resourceTitle')}
                    popupEditFormI18nKeys={{
                        createSuccess: 'page.entitats.accio.crearOk',
                        updateSuccess: 'page.entitats.accio.modificarOk',
                        deleteSuccess: 'page.entitats.accio.esborrarOk',
                    }}
                    // Les accions de la fila són només les del menú (veure useEntitatAccions): s'amaguen
                    // les que la graella hi posa pel seu compte per no duplicar modificar i esborrar.
                    rowHideUpdateButton
                    rowHideDeleteButton
                    rowAdditionalActions={accions}
                />
            </CardPage>
        </GridPage>
    );
};

export default EntitatGrid;
