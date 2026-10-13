import React from 'react';
import { useTranslation } from 'react-i18next';
import { GridPage, type MuiDataGridColDef, useMuiDataGridApiRef } from 'reactlib';
import { CardPage } from '../../components/CardData';
import StyledMuiGrid, { ToolbarButton } from '../../components/StyledMuiGrid';
import { formatDate } from '../../util/dateUtils';
import IntegracioFilter from './IntegracioFilter';
import { useIntegracioTabs } from './IntegracioTabs';
import useIntegracioDetail from './IntegracioDetail';
import { IntegracioEstat } from './IntegracioEstat';

const columns: MuiDataGridColDef[] = [
    {
        field: 'data',
        flex: 0.6,
        valueFormatter: (value: string) => (value ? formatDate(value, 'DD/MM/YYYY HH:mm:ss') : ''),
    },
    { field: 'descripcio', flex: 1.5 },
    { field: 'tipus', flex: 0.5 },
    { field: 'endpoint', flex: 1 },
    {
        field: 'tempsResposta',
        flex: 0.5,
        valueFormatter: (value: number) => (value != null ? `${value} ms` : ''),
    },
    {
        field: 'estat',
        flex: 0.4,
        renderCell: (params: any) => (
            <IntegracioEstat
                value={params?.row?.estat}
                excepcioMessage={params?.row?.excepcioMessage}
            />
        ),
    },
];

const sortModel = [{ field: 'data', sort: 'desc' as const }];

/**
 * Monitor d'integracions: peticions a Arxiu, Custòdia Antiga, validació de firmes i conversió de documents,
 * amb una pestanya per integració, filtre i detall de cada petició.
 */
export const IntegracioGrid: React.FC = () => {
    const { t } = useTranslation();
    const apiRef = useMuiDataGridApiRef();
    const [formFilter, setFormFilter] = React.useState<string>();
    const { value: tab, tabElement } = useIntegracioTabs();
    const { show: mostrarDetall, component: detailDialog } = useIntegracioDetail();

    // La integració de la pestanya viatja com a "named query" i no dins `filter`
    const namedQueries = React.useMemo(() => [tab], [tab]);

    return (
        <GridPage>
            <CardPage title={t('page.integracio.grid.title')}>
                <IntegracioFilter onSpringFilterChange={setFormFilter} />
                <StyledMuiGrid
                    readOnly
                    toolbarHideCreate
                    toolbarShowFilterCount
                    rowHideUpdateButton
                    rowHideDeleteButton
                    resourceName="monitorIntegracioResource"
                    apiRef={apiRef}
                    columns={columns}
                    filter={formFilter}
                    namedQueries={namedQueries}
                    paginationActive
                    sortModel={sortModel}
                    onRowClick={(params: any) => mostrarDetall(params?.row?.id, params?.row)}
                    rowAdditionalActions={[
                        {
                            label: t('page.integracio.detail.title'),
                            icon: 'info',
                            showInMenu: false,
                            onClick: (id: any, row: any) => mostrarDetall(id, row),
                        },
                    ]}
                    toolbarElementsWithPositions={[
                        { position: 0, element: tabElement },
                        {
                            position: 2,
                            element: (
                                // Provisional: el diagnòstic ràpid encara no està implementat.
                                <ToolbarButton
                                    icon="monitor_heart"
                                    title={t('page.integracio.diagnostic.titol')}
                                    onClick={() =>
                                        window.alert(t('page.integracio.diagnostic.enConstruccio'))
                                    }
                                >
                                    {t('page.integracio.diagnostic.boto')}
                                </ToolbarButton>
                            ),
                        },
                    ]}
                />
                {detailDialog}
            </CardPage>
        </GridPage>
    );
};

export default IntegracioGrid;
