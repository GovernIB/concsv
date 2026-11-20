import React from 'react';
import { useTranslation } from 'react-i18next';
import { GridPage, type MuiDataGridColDef, useFormContext, useMuiDataGridApiRef } from 'reactlib';
import { useDocumentExclosAccions } from './DocumentExclosAccions';
import DocumentExclosEstat from './DocumentExclosEstat';
import DocumentExclosFilter from './DocumentExclosFilter';
import ScreenFrame from '../../components/ScreenFrame';
import StyledMuiGrid from '../../components/StyledMuiGrid';
import { formatDate } from '../../util/dateUtils';
import { icons } from '../../util/icons';
import { Grid, Icon, IconButton } from '@mui/material';
import GridFormField from '../../components/GridFormField';

const FORMAT_DATA = 'DD/MM/YYYY HH:mm:ss';

/**
 * Formulari de la fila: el valor és l'únic editable; en modificar, les dades que omple el servidor
 * consultant el document es veuen deshabilitades, cadascuna amb la seva mida.
 */
const DocumentExclosFormContent: React.FC = () => {
    const { data } = useFormContext();
    const modificant = data?.id != null;
    return (
        <Grid container spacing={2}>
            <GridFormField size={12} name="valor" />
            {modificant && (
                <>
                    <GridFormField size={6} name="nomDocument" disabled />
                    <GridFormField size={3} name="dataCaptura" disabled />
                    <GridFormField size={3} name="dataActualitzacio" disabled />
                    <GridFormField size={7} name="csv" disabled />
                    <GridFormField size={5} name="uuid" disabled />
                    <GridFormField size={3} name="organ" disabled />
                    <GridFormField size={3} name="ubicacio" disabled />
                    <GridFormField size={3} name="origen" disabled />
                    <GridFormField size={3} name="estatActualitzacio" disabled />
                    <GridFormField size={12} name="missatgeActualitzacio" disabled />
                </>
            )}
        </Grid>
    );
};

const sortModel = [{ field: 'valor', sort: 'asc' as const }];

const DocumentExclosGrid: React.FC = () => {
    const { t } = useTranslation();
    const apiRef = useMuiDataGridApiRef();
    const [formFilter, setFormFilter] = React.useState<string>();
    const refresh = () => apiRef.current?.refresh?.();
    const { accions, accionsMassives, botonsToolbar, dialegImportacio } = useDocumentExclosAccions(refresh);

    const columns: MuiDataGridColDef[] = React.useMemo(
        () => [
            { field: 'valor', flex: 2, minWidth: 260 },
            { field: 'nomDocument', flex: 1.2, minWidth: 160 },
            {
                field: 'dataCaptura',
                flex: 0.7,
                minWidth: 150,
                valueFormatter: (value: string) => (value ? formatDate(value, FORMAT_DATA) : ''),
            },
            {
                field: 'estatActualitzacio',
                flex: 0.6,
                minWidth: 130,
                renderCell: (params: { row?: { estatActualitzacio?: string; missatgeActualitzacio?: string } }) => (
                    <DocumentExclosEstat
                        value={params?.row?.estatActualitzacio}
                        missatge={params?.row?.missatgeActualitzacio}
                    />
                ),
            },
            {
                field: 'dataActualitzacio',
                flex: 0.7,
                minWidth: 150,
                valueFormatter: (value: string) => (value ? formatDate(value, FORMAT_DATA) : ''),
            },
            {
                field: 'enllac',
                width: 80,
                sortable: false,
                align: 'center',
                headerAlign: 'center',
                renderCell: (params: { row?: { enllac?: string } }) => {
                    const enllac = params?.row?.enllac;
                    const titol = t('page.documentsExclosos.grid.obreEnllac');
                    return enllac ? (
                        <IconButton
                            size="small"
                            component="a"
                            href={enllac}
                            target="_blank"
                            rel="noopener noreferrer"
                            title={titol}
                            aria-label={titol}
                        >
                            <Icon fontSize="small">{icons.obreEnllac}</Icon>
                        </IconButton>
                    ) : null;
                },
            },
        ],
        [t]
    );

    return (
        <GridPage>
            <ScreenFrame icon={icons.documentExclos} title={t('page.documentsExclosos.grid.title')}>
                <DocumentExclosFilter onSpringFilterChange={setFormFilter} />
                <StyledMuiGrid
                    filter={formFilter}
                    toolbarShowFilterCount
                    toolbarCreateTitle={t('page.documentsExclosos.accio.nova')}
                    resourceName="documentExclosResource"
                    apiRef={apiRef}
                    columns={columns}
                    sortModel={sortModel}
                    paginationActive
                    popupEditActive
                    popupEditFormContent={<DocumentExclosFormContent />}
                    popupEditFormDialogComponentProps={{ fullWidth: true, maxWidth: 'lg' }}
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
            </ScreenFrame>
        </GridPage>
    );
};

export default DocumentExclosGrid;
