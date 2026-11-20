import React from 'react';
import Chip from '@mui/material/Chip';
import { useTranslation } from 'react-i18next';
import {
    GridPage,
    useBaseAppContext,
    useConfirmDialogButtons,
    useMuiDataGridApiRef,
    useResourceApiService,
    type MuiDataGridColDef,
} from 'reactlib';
import ScreenFrame from '../../components/ScreenFrame';
import StyledMuiGrid, { ToolbarButton } from '../../components/StyledMuiGrid';
import TonalLinearProgress from '../../components/TonalLinearProgress';
import { icons } from '../../util/icons';
import { formatDate } from '../../util/dateUtils';
import { tonal, tonalNeutre } from '../../util/tonal';
import ExecucioMassivaEstat from './ExecucioMassivaEstat';
import useExecucioMassivaDetail, { progresExecucio, type Execucio } from './ExecucioMassivaDetail';

const RESOURCE_NAME = 'execucioMassivaResource';
const ACCIO_CANCELAR = 'CANCELAR';
const RESULTAT_CANCELADA = 'CANCELADA';
const INTERVAL_REFRESC_MS = 10_000;
const FORMAT_DATA = 'DD/MM/YYYY HH:mm:ss';

const sortModel = [{ field: 'dataInici', sort: 'desc' as const }];

/** Recompte dins un xip tonal: l'estil de color és el que el fa destacar. */
const XipRecompte: React.FC<{ valor?: number; color?: 'info' | 'error' }> = ({ valor = 0, color }) => (
    <Chip label={valor} size="small" sx={color ? tonal(color) : tonalNeutre} />
);

type ErrorApi = { description?: string; message?: string };

/**
 * Execucions massives: les accions que s'executen en segon pla sobre molts elements.
 * És una pantalla de consulta: les execucions les crea l'acció que les origina. De cada una es pot veure el detall
 * (els elements amb el seu estat) i, mentre no ha acabat, cancel·lar-la.
 */
export const ExecucioMassivaGrid: React.FC = () => {
    const { t } = useTranslation();
    const apiRef = useMuiDataGridApiRef();
    const { temporalMessageShow, messageDialogShow } = useBaseAppContext();
    const confirmDialogButtons = useConfirmDialogButtons();
    const { isReady: apiIsReady, artifactAction: apiArtifactAction } = useResourceApiService(RESOURCE_NAME);
    const [autoRefresh, setAutoRefresh] = React.useState(false);
    const { show: mostrarDetall, refresh: refrescaDetall, component: detailDialog } = useExecucioMassivaDetail();

    const refresh = () => apiRef.current?.refresh?.();

    // El refresc automàtic recarrega el llistat i el detall obert (si n'hi ha). La funció de l'efecte no es torna a
    // crear a cada render, per això el que ha de ser actual es llegeix d'una referència.
    const refrescaTot = React.useRef<() => void>(() => undefined);
    refrescaTot.current = () => {
        refresh();
        refrescaDetall();
    };

    React.useEffect(() => {
        if (!autoRefresh) {
            return undefined;
        }
        const interval = setInterval(() => refrescaTot.current(), INTERVAL_REFRESC_MS);
        return () => clearInterval(interval);
    }, [autoRefresh]);

    const cancela = (id: unknown) => {
        if (!apiIsReady) {
            return;
        }
        messageDialogShow(
            t('page.execucionsMassives.accio.cancelaTitol'),
            t('page.execucionsMassives.accio.confirmarCancelacio', { id }),
            confirmDialogButtons,
            { maxWidth: 'sm', fullWidth: true }
        ).then((confirmat: unknown) => {
            if (!confirmat) {
                return;
            }
            apiArtifactAction(id, { code: ACCIO_CANCELAR })
                .then((resposta: unknown) => {
                    const resultat = (resposta as { resultat?: string } | undefined)?.resultat;
                    refrescaTot.current();
                    temporalMessageShow(
                        null,
                        t(
                            resultat === RESULTAT_CANCELADA
                                ? 'page.execucionsMassives.accio.cancelada'
                                : 'page.execucionsMassives.accio.cancelacioDemanada'
                        ),
                        resultat === RESULTAT_CANCELADA ? 'success' : 'info'
                    );
                })
                .catch((error: ErrorApi) =>
                    temporalMessageShow(
                        t('page.execucionsMassives.accio.error'),
                        error?.description ?? error?.message ?? '',
                        'error'
                    )
                );
        });
    };

    const columns: MuiDataGridColDef[] = React.useMemo(
        () => [
            {
                field: 'tipus',
                flex: 1.2,
                valueFormatter: (value: string) => (value ? t(`page.execucionsMassives.tipusValor.${value}`) : ''),
            },
            {
                field: 'estat',
                flex: 0.7,
                minWidth: 140,
                maxWidth: 140,
                renderCell: (params: { row?: Execucio }) => (
                    <ExecucioMassivaEstat
                        value={params?.row?.estat}
                        cancelacioData={params?.row?.cancelacioData}
                        dataFi={params?.row?.dataFi}
                    />
                ),
            },
            {
                // Nombre d'elements de l'execució
                field: 'total',
                headerName: t('page.execucionsMassives.grid.elements'),
                type: 'number',
                align: 'center',
                headerAlign: 'center',
                flex: 0.4,
                minWidth: 90,
                sortable: false,
                renderCell: (params: { row?: Execucio }) => <XipRecompte valor={params?.row?.total} color="info" />,
            },
            {
                // Percentatge d'elements tractats
                field: 'finalitzats',
                headerName: t('page.execucionsMassives.grid.progres'),
                flex: 1,
                minWidth: 160,
                sortable: false,
                renderCell: (params: { row?: Execucio }) => {
                    const { percentatge } = progresExecucio(params?.row);
                    return (
                        <TonalLinearProgress
                            value={percentatge}
                            color={(params?.row?.errors ?? 0) > 0 ? 'warning' : 'success'}
                            boxSx={{ my: 'auto' }}
                            sx={{ height: 22 }}
                        >
                            {`${percentatge}%`}
                        </TonalLinearProgress>
                    );
                },
            },
            {
                field: 'errors',
                type: 'number',
                align: 'center',
                headerAlign: 'center',
                flex: 0.4,
                minWidth: 80,
                sortable: false,
                // Vermell només si n'hi ha; amb 0 queda un xip neutre discret
                renderCell: (params: { row?: Execucio }) => (
                    <XipRecompte
                        valor={params?.row?.errors}
                        color={(params?.row?.errors ?? 0) > 0 ? 'error' : undefined}
                    />
                ),
            },
            {
                field: 'dataInici',
                flex: 0.8,
                minWidth: 150,
                valueFormatter: (value: string) => (value ? (formatDate(value, FORMAT_DATA) ?? '') : ''),
            },
            {
                field: 'dataFi',
                flex: 0.8,
                minWidth: 150,
                valueFormatter: (value: string) => (value ? (formatDate(value, FORMAT_DATA) ?? '') : ''),
            },
            { field: 'createdByFullName', flex: 1, minWidth: 180, sortable: false },
        ],
        [t]
    );

    return (
        <GridPage>
            <ScreenFrame icon={icons.execucioMassiva} title={t('page.execucionsMassives.grid.title')}>
                <StyledMuiGrid
                    readOnly
                    toolbarHideCreate
                    rowHideUpdateButton
                    rowHideDeleteButton
                    resourceName={RESOURCE_NAME}
                    apiRef={apiRef}
                    columns={columns}
                    paginationActive
                    sortModel={sortModel}
                    onRowClick={(params: { row?: Execucio }) => mostrarDetall(params?.row?.id as number)}
                    rowAdditionalActions={[
                        {
                            label: t('page.execucionsMassives.accio.detall'),
                            icon: icons.detall,
                            showInMenu: true,
                            onClick: (id: unknown) => mostrarDetall(id as number),
                        },
                        {
                            label: t('page.execucionsMassives.accio.cancela'),
                            title: t('page.execucionsMassives.accio.cancelaTitol'),
                            icon: icons.cancela,
                            showInMenu: true,
                            // Només mentre l'execució no ha acabat ni s'ha demanat ja la cancel·lació
                            hidden: (row: Execucio) => !row?.cancelable,
                            onClick: (id: unknown) => cancela(id),
                        },
                    ]}
                    toolbarElementsWithPositions={[
                        {
                            position: 2,
                            element: (
                                <ToolbarButton
                                    icon={autoRefresh ? icons.refrescaAturada : icons.refrescaAutomatic}
                                    title={t(
                                        autoRefresh
                                            ? 'page.execucionsMassives.grid.desactivaRefresc'
                                            : 'page.execucionsMassives.grid.activaRefresc'
                                    )}
                                    variant="contained"
                                    aria-pressed={autoRefresh}
                                    onClick={() => setAutoRefresh((actiu) => !actiu)}
                                >
                                    {t(
                                        autoRefresh
                                            ? 'page.execucionsMassives.grid.desactivaRefresc'
                                            : 'page.execucionsMassives.grid.activaRefresc'
                                    )}
                                </ToolbarButton>
                            ),
                        },
                    ]}
                />
                {detailDialog}
            </ScreenFrame>
        </GridPage>
    );
};

export default ExecucioMassivaGrid;
