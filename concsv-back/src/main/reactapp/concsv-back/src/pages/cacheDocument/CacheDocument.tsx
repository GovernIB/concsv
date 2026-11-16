import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Typography } from '@mui/material';
import { GridPage, type MuiDataGridColDef, useMuiDataGridApiRef } from 'reactlib';
import ScreenFrame from '../../components/ScreenFrame';
import StyledMuiGrid from '../../components/StyledMuiGrid';
import { icons } from '../../util/icons';
import { formatDate } from '../../util/dateUtils';
import CacheDocumentFilter from './CacheDocumentFilter';
import { useCacheDocumentAccions } from './CacheDocumentAccions';

const CARPETES = ['INFO', 'ORIGINAL', 'IMPRIMIBLE'] as const;

const FORMAT_DATA = 'DD/MM/YYYY HH:mm:ss';

/** Mida en bytes en una unitat llegible (KB, MB, ...). */
const formatMida = (bytes?: number | null): string => {
    if (bytes == null) {
        return '';
    }
    const unitats = ['B', 'KB', 'MB', 'GB'];
    let valor = bytes;
    let unitat = 0;
    while (valor >= 1024 && unitat < unitats.length - 1) {
        valor /= 1024;
        unitat++;
    }
    return `${unitat === 0 ? valor : valor.toFixed(1)} ${unitats[unitat]}`;
};

const sortModel = [{ field: 'darreraConsulta', sort: 'desc' as const }];

/**
 * Cache de documents del servidor: llistat dels documents que hi ha a cada carpeta de la cache, amb la darrera
 * consulta i la data d'expiració. Es poden filtrar per carpeta i per UUID / CSV, i esborrar des de les accions
 * massives (amb "Selecciona-ho tot" s'esborra tot el que mostra el filtre).
 */
export const CacheDocument: React.FC = () => {
    const { t } = useTranslation();
    const apiRef = useMuiDataGridApiRef();
    const [filtre, setFiltre] = React.useState<string>();
    const refresh = () => apiRef.current?.refresh?.();
    const { accionsMassives } = useCacheDocumentAccions(refresh);

    const columns: MuiDataGridColDef[] = React.useMemo(
        () => [
            { field: 'carpeta', flex: 0.8 },
            { field: 'identificador', flex: 1.2 },
            // Nom real del document: només es coneix a INFO (es llegeix de l'objecte serialitzat); a
            // ORIGINAL i IMPRIMIBLE queda buit a propòsit, veure CacheDocumentHelper.
            { field: 'nomDocument', flex: 1.3 },
            { field: 'tipusIdentificador', flex: 0.5 },
            { field: 'idioma', flex: 0.4 },
            { field: 'mida', flex: 0.4, valueFormatter: (value: number) => formatMida(value) },
            {
                field: 'darreraConsulta',
                flex: 0.8,
                minWidth: 160,
                valueFormatter: (value: string) => (value ? formatDate(value, FORMAT_DATA) : ''),
            },
            {
                field: 'expiracio',
                flex: 0.9,
                minWidth: 140,
                renderCell: (params: { row?: { expiracio?: string } }) => {
                    const expiracio: string | undefined = params?.row?.expiracio;
                    if (!expiracio) {
                        return (
                            <Typography variant="body2" color="text.secondary">
                                {t('page.cacheDocuments.grid.senseExpiracio')}
                            </Typography>
                        );
                    }
                    const caducada = new Date(expiracio).getTime() < Date.now();
                    return (
                        <Typography variant="body2" color={caducada ? 'warning.main' : 'text.primary'}>
                            {formatDate(expiracio, FORMAT_DATA)}
                            {caducada ? ` (${t('page.cacheDocuments.grid.caducada')})` : ''}
                        </Typography>
                    );
                },
            },
        ],
        [t]
    );

    return (
        <GridPage>
            <ScreenFrame icon={icons.cacheDocument} title={t('page.cacheDocuments.title')}>
                <Box sx={{ px: 1, pb: 1 }}>
                    <Typography color="text.secondary">{t('page.cacheDocuments.descripcio.intro')}</Typography>
                    <Box component="ul" sx={{ color: 'text.secondary', my: 1, pl: 3 }}>
                        {CARPETES.map((carpeta) => (
                            <li key={carpeta}>
                                <Typography component="span" color="text.primary" sx={{ fontWeight: 'bold' }}>
                                    {carpeta}
                                </Typography>
                                {': ' + t(`page.cacheDocuments.descripcio.${carpeta.toLowerCase()}`)}
                            </li>
                        ))}
                    </Box>
                    <Typography color="text.secondary">{t('page.cacheDocuments.descripcio.final')}</Typography>
                </Box>
                <CacheDocumentFilter onSpringFilterChange={setFiltre} />
                <StyledMuiGrid
                    toolbarHideCreate
                    toolbarShowFilterCount
                    rowHideUpdateButton
                    rowHideDeleteButton
                    resourceName="cacheDocumentResource"
                    apiRef={apiRef}
                    columns={columns}
                    filter={filtre}
                    sortModel={sortModel}
                    paginationActive
                    selectionActive
                    toolbarMassiveActions={accionsMassives}
                />
            </ScreenFrame>
        </GridPage>
    );
};

export default CacheDocument;
