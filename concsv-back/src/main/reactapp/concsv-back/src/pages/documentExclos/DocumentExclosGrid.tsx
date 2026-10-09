import React from 'react';
import { useTranslation } from 'react-i18next';
import { GridPage, type MuiDataGridColDef, useMuiDataGridApiRef } from 'reactlib';
import { useDocumentExclosAccions } from './DocumentExclosAccions';
import { CardPage } from '../../components/CardData';
import StyledMuiGrid from '../../components/StyledMuiGrid';
import { Grid } from '@mui/material';
import GridFormField from '../../components/GridFormField';

const DocumentExclosFormContent: React.FC = () => (
    <Grid container spacing={2}>
        <GridFormField size={12} name="valor" />
    </Grid>
);

const columns: MuiDataGridColDef[] = [{ field: 'valor', flex: 1 }];

const sortModel = [{ field: 'valor', sort: 'asc' as const }];

const DocumentExclosGrid: React.FC = () => {
    const { t } = useTranslation();
    const apiRef = useMuiDataGridApiRef();
    const refresh = () => apiRef.current?.refresh?.();
    const { accions, accionsMassives, botonsToolbar, dialegImportacio } = useDocumentExclosAccions(refresh);

    return (
        <GridPage>
            <CardPage title={t('page.documentsExclosos.grid.title')}>
                <StyledMuiGrid
                    toolbarCreateTitle={t('page.documentsExclosos.accio.nova')}
                    resourceName="documentExclosResource"
                    apiRef={apiRef}
                    columns={columns}
                    sortModel={sortModel}
                    toolbarShowQuickFilter
                    paginationActive
                    popupEditActive
                    popupEditFormContent={<DocumentExclosFormContent />}
                    popupEditFormDialogResourceTitle={t('page.documentsExclosos.form.resourceTitle')}
                    popupEditFormI18nKeys={{
                        createSuccess: 'page.documentsExclosos.accio.crearOk',
                        updateSuccess: 'page.documentsExclosos.accio.modificarOk',
                        deleteSuccess: 'page.documentsExclosos.accio.esborrarOk',
                    }}
                    selectionActive
                    rowHideUpdateButton
                    rowHideDeleteButton
                    rowAdditionalActions={accions}
                    toolbarMassiveActions={accionsMassives}
                    toolbarElementsWithPositions={botonsToolbar}
                />
                {dialegImportacio}
            </CardPage>
        </GridPage>
    );
};

export default DocumentExclosGrid;
