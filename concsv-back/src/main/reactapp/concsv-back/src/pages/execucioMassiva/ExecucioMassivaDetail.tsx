import React from 'react';
import { useTranslation } from 'react-i18next';
import { MuiDialog, useCloseDialogButtons, useMuiDataGridApiRef, type MuiDataGridColDef } from 'reactlib';
import { ScreenFrameContext } from '../../components/ScreenFrameContext';
import StyledMuiGrid from '../../components/StyledMuiGrid';
import { icons } from '../../util/icons';
import { formatDate } from '../../util/dateUtils';
import * as builder from '../../util/springFilterUtils';
import { ExecucioMassivaContingutEstat } from './ExecucioMassivaEstat';

const FORMAT_DATA = 'DD/MM/YYYY HH:mm:ss';
const BOTO_REFRESCA = 'refresca';
/** Part de l'alçada de la finestra que ocupa el grid: el scroll és dins el grid, amb la capçalera i la paginació sempre visibles. */
const PART_ALCADA_GRID = 0.55;
const ALCADA_MINIMA_GRID = 320;

/** Execució massiva tal com la serveix el backend (ExecucioMassivaResource). */
export type Execucio = {
    id?: number;
    tipus?: string;
    estat?: string;
    dataInici?: string;
    dataFi?: string;
    cancelacioData?: string;
    createdBy?: string;
    /** "Nom (codi)" de l'usuari que l'ha creada. */
    createdByFullName?: string;
    total?: number;
    finalitzats?: number;
    errors?: number;
    pendents?: number;
    cancelats?: number;
    cancelable?: boolean;
};

/** Fila del llistat d'elements (ExecucioMassivaContingutResource). */
type ContingutFila = { estat?: string; error?: string };

const sortModel = [{ field: 'dataFi', sort: 'desc' as const }];

/** Elements tractats (acabats o amb error) d'una execució i percentatge sobre el total: els cancel·lats no s'hi compten. */
export const progresExecucio = (execucio?: Execucio): { fets: number; total: number; percentatge: number } => {
    const total = execucio?.total ?? 0;
    const fets = (execucio?.finalitzats ?? 0) + (execucio?.errors ?? 0);
    return { fets, total, percentatge: total > 0 ? Math.round((fets / total) * 100) : 0 };
};

/**
 * Diàleg amb el detall d'una execució massiva: només el llistat dels seus elements amb el seu estat i, si han
 * fallat, el motiu. `refresh` el torna a carregar (per al refresc automàtic de la pantalla).
 */
export const useExecucioMassivaDetail = () => {
    const { t } = useTranslation();
    const closeButtons = useCloseDialogButtons();
    // Refresca (a l'esquerra de Tanca): l'usuari decideix quan recarregar els elements, no es fa sol
    const buttons = React.useMemo(
        () => [
            {
                value: BOTO_REFRESCA,
                text: t('common.refresh'),
                icon: icons.refresca,
                componentProps: { variant: 'outlined' as const },
            },
            ...closeButtons,
        ],
        [t, closeButtons]
    );
    const contingutsApiRef = useMuiDataGridApiRef();
    const [open, setOpen] = React.useState(false);
    const [id, setId] = React.useState<number>();
    
    // Alçada fixa (no creix amb el nombre de files): un grid més alt que la finestra faria fer scroll al diàleg
    const alturaGrid = React.useMemo(
        () => Math.max(ALCADA_MINIMA_GRID, Math.round(window.innerHeight * PART_ALCADA_GRID)),
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [open]
    );

    const handleOpen = (execucioId: number) => {
        setId(execucioId);
        setOpen(true);
    };

    const handleClose = (reason?: string) => {
        if (reason !== 'backdropClick') {
            setOpen(false);
        }
    };

    const refresh = () => {
        if (open && id != null) {
            contingutsApiRef.current?.refresh?.();
        }
    };

    const columns: MuiDataGridColDef[] = React.useMemo(
        () => [
            {
                field: 'elementTipus',
                flex: 1,
                valueFormatter: (value: string) =>
                    value ? t(`page.execucionsMassives.continguts.tipusElement.${value}`) : '',
            },
            { field: 'elementNom', flex: 3 },
            {
                field: 'estat',
                flex: 1,
                minWidth: 160,
                renderCell: (params: { row?: ContingutFila }) => (
                    <ExecucioMassivaContingutEstat value={params?.row?.estat} error={params?.row?.error} />
                ),
            },
            {
                field: 'dataFi',
                flex: 1,
                minWidth: 160,
                valueFormatter: (value: string) => (value ? (formatDate(value, FORMAT_DATA) ?? '') : ''),
            },
        ],
        [t]
    );

    const dialog = (
        <MuiDialog
            open={open}
            closeCallback={handleClose}
            title={t('page.execucionsMassives.detail.title')}
            componentProps={{ fullWidth: true, maxWidth: 'lg' }}
            buttons={buttons}
            buttonCallback={(value: unknown) => (value === BOTO_REFRESCA ? refresh() : handleClose())}
        >
            {open && id != null && (
                // El diàleg és dins el ScreenFrame de la pantalla per context de React (els portals el travessen):
                // sense això el grid s'hi enganxaria i el recompte de filtres sortiria al títol de la pantalla.
                <ScreenFrameContext.Provider value={undefined}>
                    <StyledMuiGrid
                        readOnly
                        toolbarHide
                        rowHideUpdateButton
                        rowHideDeleteButton
                        height={alturaGrid}
                        resourceName="execucioMassivaContingutResource"
                        apiRef={contingutsApiRef}
                        columns={columns}
                        filter={builder.eq('execucioMassiva.id', id)}
                        paginationActive
                        sortModel={sortModel}
                    />
                </ScreenFrameContext.Provider>
            )}
        </MuiDialog>
    );

    return { show: handleOpen, close: handleClose, refresh, component: dialog };
};

export default useExecucioMassivaDetail;
