import React, { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button, Grid, Icon, IconButton, InputAdornment, TextField, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import { SimpleTreeView } from '@mui/x-tree-view/SimpleTreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import { GridPage, useBaseAppContext, useConfirmDialogButtons, useDebounce, useResourceApiService } from 'reactlib';
import { CardPage } from '../../components/CardData';
import { PropietatsProps } from './PropietatsProps';
import type { PropietatGrup, PropietatItem } from './PropietatsTypes';
import { missatgeError } from './PropietatsUtils';

const ACCIO_SINCRONITZA = 'SYNC_JBOSS';

/** Camp de filtre ràpid amb retard (debounce) i botó per netejar-lo. */
const QuickFilter: React.FC<{ onChange: (quickFilter: string | undefined) => void }> = ({ onChange }) => {
    const [quickFilter, setQuickFilter] = React.useState<string>('');
    const quickFilterDebounced = useDebounce(quickFilter);

    useEffect(() => {
        onChange(quickFilterDebounced);
        // `onChange` és un setState del pare: no cal tornar a executar-ho si canvia la referència.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [quickFilterDebounced]);

    return (
        <TextField
            value={quickFilter}
            onChange={(event) => setQuickFilter(event.target.value)}
            variant="outlined"
            size="small"
            slotProps={{
                input: {
                    startAdornment: (
                        <InputAdornment position="start">
                            <Icon fontSize="small">search</Icon>
                        </InputAdornment>
                    ),
                    endAdornment: quickFilter && (
                        <InputAdornment position="end">
                            <IconButton size="small" onClick={() => setQuickFilter('')}>
                                <Icon fontSize="inherit">clear</Icon>
                            </IconButton>
                        </InputAdornment>
                    ),
                },
            }}
        />
    );
};

/**
 * Manteniment de propietats configurables.
 *
 * Es carreguen d'una vegada tots els grups i totes les propietats de l'àmbit (en són pocs) i el
 * filtre ràpid s'aplica al client. Cada propietat es carrega i es desa amb el seu propi formulari.
 * El botó "Sincronitza" copia a la base de dades els valors dels fitxers del servidor.
 */
export const Propietats: React.FC = () => {
    const { t } = useTranslation();
    const { temporalMessageShow, messageDialogShow } = useBaseAppContext();
    const confirmDialogButtons = useConfirmDialogButtons();
    const [quickFilter, setQuickFilter] = useState<string>();
    // Es torna a carregar tot (llista i formularis de les files) quan canvia
    const [refreshKey, setRefreshKey] = useState(0);
    const [sincronitzant, setSincronitzant] = useState(false);

    const {
        isReady: isConfigReady,
        find: apiConfigFind,
        artifactAction: apiConfigAction,
    } = useResourceApiService('configResource');
    const [configs, setConfigs] = useState<PropietatItem[]>();

    useEffect(() => {
        if (isConfigReady) {
            apiConfigFind({ sorts: ['position,asc'], unpaged: true })
                .then((response: { rows: PropietatItem[] }) => setConfigs(response.rows))
                .catch((error: unknown) => temporalMessageShow(null, missatgeError(error), 'error'));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isConfigReady, refreshKey]);

    /** Manté la llista al dia (filtre ràpid) quan una fila es desa. */
    const alDesar = (desada: PropietatItem) =>
        setConfigs((actual) => actual?.map((c) => (c.id === desada.id ? { ...c, ...desada } : c)));

    const filteredProps = useMemo(() => {
        if (!configs) {
            return [];
        }
        if (!quickFilter) {
            return configs;
        }
        const text = quickFilter.toLowerCase();
        return configs.filter(
            (c) =>
                c.key?.toLowerCase().includes(text) ||
                c.value?.toLowerCase().includes(text) ||
                c.description?.toLowerCase().includes(text)
        );
    }, [quickFilter, configs]);

    const { isReady: isGroupReady, find: apiGroupFind } = useResourceApiService('configGroupResource');
    const [configGroups, setConfigGroups] = useState<PropietatGrup[]>();
    const [selectedGroupId, setSelectedGroupId] = useState<string>();

    useEffect(() => {
        if (isGroupReady) {
            apiGroupFind({ sorts: ['position,asc'], unpaged: true })
                .then((response: { rows: PropietatGrup[] }) => {
                    setConfigGroups(response.rows);
                    setSelectedGroupId(response.rows.length ? response.rows[0].id : undefined);
                })
                .catch((error: unknown) => temporalMessageShow(null, missatgeError(error), 'error'));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isGroupReady]);

    // Només es mostren els grups amb alguna propietat que passa el filtre (i el pare d'aquests).
    const filteredGroups = useMemo(() => {
        if (!configGroups || filteredProps.length === 0) {
            return [];
        }
        const amb = configGroups.filter((g) => filteredProps.some((p) => g.id === p.group.id));
        return configGroups.filter((g) => amb.some((g2) => g2.id === g.id || g2.parent?.id === g.id));
    }, [filteredProps, configGroups]);

    // Si el filtre deixa fora el grup seleccionat, se selecciona el primer que queda.
    const grupsArrel = useMemo(() => filteredGroups.filter((g) => g.parent == null), [filteredGroups]);

    useEffect(() => {
        if (grupsArrel.length && !grupsArrel.some((g) => g.id === selectedGroupId)) {
            setSelectedGroupId(grupsArrel[0].id);
        }
    }, [grupsArrel, selectedGroupId]);

    const sincronitza = () => {
        messageDialogShow(
            t('page.propietats.accio.sync.label'),
            t('page.propietats.accio.sync.confirm'),
            confirmDialogButtons,
            { maxWidth: 'sm', fullWidth: true }
        ).then((confirmat: unknown) => {
            if (!confirmat) {
                return;
            }
            setSincronitzant(true);
            apiConfigAction(undefined, { code: ACCIO_SINCRONITZA })
                .then((resultat: unknown) => {
                    // L'acció retorna el nombre de propietats actualitzades
                    temporalMessageShow(
                        null,
                        typeof resultat === 'number'
                            ? t('page.propietats.accio.sync.okCount', { count: resultat })
                            : t('page.propietats.accio.sync.ok'),
                        'success'
                    );
                    setRefreshKey((clau) => clau + 1);
                })
                .catch((error: unknown) =>
                    temporalMessageShow(t('page.propietats.accio.sync.error'), missatgeError(error), 'error')
                )
                .finally(() => setSincronitzant(false));
        });
    };

    const senseResultats = configs !== undefined && filteredProps.length === 0;

    return (
        <GridPage>
            <CardPage title={t('page.propietats.title')}>
                <Grid container spacing={2}>
                    <Grid size={12} sx={{ px: 1 }} display={'flex'} justifyContent={'end'} gap={1}>
                        <Button
                            variant="outlined"
                            size="small"
                            sx={{ borderRadius: '4px' }}
                            disabled={sincronitzant || !isConfigReady}
                            onClick={sincronitza}
                        >
                            <Icon sx={{ mr: 0.5 }}>cached</Icon>
                            {t('page.propietats.accio.sync.label')}
                        </Button>
                        <QuickFilter onChange={setQuickFilter} />
                    </Grid>
                    {senseResultats ? (
                        <Grid size={12}>
                            <Box sx={{ textAlign: 'center', px: 2, py: 6 }}>
                                <Icon fontSize="large" color="disabled">
                                    block
                                </Icon>
                                <Typography variant="h5" color="text.secondary">
                                    {t('page.propietats.empty')}
                                </Typography>
                            </Box>
                        </Grid>
                    ) : (
                        <>
                            <Grid size={3}>
                                <Box
                                    sx={{
                                        height: '68vh',
                                        overflow: 'auto',
                                        border: '1px solid #e0e0e0',
                                        borderRadius: 1,
                                    }}
                                >
                                    <SimpleTreeView
                                        selectedItems={selectedGroupId ?? null}
                                        onSelectedItemsChange={(_event, id) => setSelectedGroupId(id || undefined)}
                                        sx={{ '& .MuiTreeItem-content': { paddingY: 1 } }}
                                    >
                                        {grupsArrel.map((g) => (
                                            <TreeItem key={g.key} itemId={g.id} label={g.description} />
                                        ))}
                                    </SimpleTreeView>
                                </Box>
                            </Grid>
                            <Grid size={9}>
                                <Box sx={{ height: '68vh' }}>
                                    <PropietatsProps
                                        root
                                        groupId={selectedGroupId}
                                        grups={filteredGroups}
                                        configs={filteredProps}
                                        highlight={quickFilter}
                                        refreshKey={refreshKey}
                                        onSaved={alDesar}
                                    />
                                </Box>
                            </Grid>
                        </>
                    )}
                </Grid>
            </CardPage>
        </GridPage>
    );
};

export default Propietats;
