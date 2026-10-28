import React from 'react';
import { useTranslation } from 'react-i18next';
import { Icon, IconButton, InputAdornment, TextField, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import { alpha } from '@mui/material/styles';
import { SimpleTreeView } from '@mui/x-tree-view/SimpleTreeView';
import { TreeItem } from '@mui/x-tree-view/TreeItem';
import { GridPage, useBaseAppContext, useConfirmDialogButtons, useDebounce, useResourceApiService } from 'reactlib';
import ScreenFrame from '../../components/ScreenFrame';
import { ToolbarButton } from '../../components/StyledMuiGrid';
import { icons } from '../../util/icons';
import { MAIN_GAP, SCREEN_GRID_MIN_HEIGHT } from '../../util/layout';
import { accentColor } from '../../theme';
import { PropietatsProps } from './PropietatsProps';
import type { PropietatGrup, PropietatItem } from './PropietatsTypes';
import { missatgeError } from './PropietatsUtils';

const ACCIO_SINCRONITZA = 'SYNC_JBOSS';

/** Camp de filtre ràpid amb retard (debounce) i botó per netejar-lo. */
const QuickFilter: React.FC<{ onChange: (quickFilter: string | undefined) => void }> = ({ onChange }) => {
    const [quickFilter, setQuickFilter] = React.useState<string>('');
    const quickFilterDebounced = useDebounce(quickFilter);

    React.useEffect(() => {
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
            // Mateixa alçada (32px) que els botons de la barra de títol i el filtre ràpid de les graelles.
            sx={{
                '& .MuiOutlinedInput-root': { height: '32px' },
                '& .MuiOutlinedInput-input': { pt: 0, pb: 0 },
            }}
            slotProps={{
                input: {
                    startAdornment: (
                        <InputAdornment position="start">
                            <Icon fontSize="small">{icons.cerca}</Icon>
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

/** Grups del menú, niuats: cada grup penja del seu pare (la clau `undefined` són els de nivell superior). */
const GroupTreeItems: React.FC<{
    parentId?: string;
    childrenByParent: Map<string | undefined, PropietatGrup[]>;
}> = ({ parentId, childrenByParent }) => (
    <>
        {childrenByParent.get(parentId)?.map((g) => (
            <TreeItem key={g.key} itemId={g.id} label={g.description}>
                {childrenByParent.has(g.id) && <GroupTreeItems parentId={g.id} childrenByParent={childrenByParent} />}
            </TreeItem>
        ))}
    </>
);

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
    const [quickFilter, setQuickFilter] = React.useState<string>();
    // Es torna a carregar tot (llista i formularis de les files) quan canvia
    const [refreshKey, setRefreshKey] = React.useState(0);
    const [sincronitzant, setSincronitzant] = React.useState(false);

    const {
        isReady: isConfigReady,
        find: apiConfigFind,
        artifactAction: apiConfigAction,
    } = useResourceApiService('configResource');
    const [configs, setConfigs] = React.useState<PropietatItem[]>();

    React.useEffect(() => {
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

    const filteredProps = React.useMemo(() => {
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
    const [configGroups, setConfigGroups] = React.useState<PropietatGrup[]>();
    const [selectedGroupId, setSelectedGroupId] = React.useState<string>();
    const [expandedGroupIds, setExpandedGroupIds] = React.useState<string[]>([]);

    React.useEffect(() => {
        if (isGroupReady) {
            // Hi pot haver grups germans amb la mateixa posició: la descripció desempata l'ordre
            apiGroupFind({ sorts: ['position,asc', 'description,asc'], unpaged: true })
                .then((response: { rows: PropietatGrup[] }) => setConfigGroups(response.rows))
                .catch((error: unknown) => temporalMessageShow(null, missatgeError(error), 'error'));
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isGroupReady]);

    const groupsById = React.useMemo(
        () => new Map<string, PropietatGrup>((configGroups ?? []).map((g) => [g.id, g])),
        [configGroups]
    );

    // Grups visibles: els que tenen propietats (filtrades) i tots els seus ancestres, a qualsevol nivell.
    const filteredGroups = React.useMemo(() => {
        if (!configGroups || filteredProps.length === 0) {
            return [];
        }
        const visibleIds = new Set<string>();
        filteredProps.forEach((p) => {
            let id: string | undefined = p.group?.id;
            while (id != null && !visibleIds.has(id)) {
                visibleIds.add(id);
                id = groupsById.get(id)?.parent?.id;
            }
        });
        return configGroups.filter((g) => visibleIds.has(g.id));
    }, [filteredProps, configGroups, groupsById]);

    // Grups visibles agrupats pel pare; els de nivell superior pengen de la clau `undefined`.
    const childrenByParent = React.useMemo(() => {
        const map = new Map<string | undefined, PropietatGrup[]>();
        filteredGroups.forEach((g) => {
            const parentId = g.parent?.id;
            map.set(parentId, [...(map.get(parentId) ?? []), g]);
        });
        return map;
    }, [filteredGroups]);

    const groupIdsWithProps = React.useMemo(() => new Set<string>(filteredProps.map((p) => p.group?.id)), [filteredProps]);

    // Els pares sense propietats pròpies són només contenidors: en fer-hi clic s'expandeixen o es
    // pleguen, però no es seleccionen.
    const isContainer = (groupId: string) => childrenByParent.has(groupId) && !groupIdsWithProps.has(groupId);

    // El grup mateix o, si no té propietats, el primer descendent que en tingui (perquè el panell no quedi buit).
    const resolveGroupWithProps = (groupId: string): string | undefined => {
        if (groupIdsWithProps.has(groupId)) {
            return groupId;
        }
        for (const child of childrenByParent.get(groupId) ?? []) {
            const resolved = resolveGroupWithProps(child.id);
            if (resolved != null) {
                return resolved;
            }
        }
        return undefined;
    };

    // Manté sempre seleccionat un grup visible i amb propietats (càrrega inicial i filtre ràpid).
    React.useEffect(() => {
        if (filteredGroups.length === 0) {
            return;
        }
        const isSelectedVisible = filteredGroups.some((g) => g.id === selectedGroupId);
        const candidates = isSelectedVisible
            ? [selectedGroupId as string]
            : (childrenByParent.get(undefined) ?? []).map((g) => g.id);
        for (const candidate of candidates) {
            const resolved = resolveGroupWithProps(candidate);
            if (resolved != null) {
                if (resolved !== selectedGroupId) {
                    setSelectedGroupId(resolved);
                }
                return;
            }
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filteredGroups, selectedGroupId]);

    // En seleccionar un grup només queda oberta la seva branca (els pares); si no en té, es plega tot el
    // menú. Amb el filtre ràpid actiu es conserva el que s'ha desplegat per les coincidències.
    React.useEffect(() => {
        const ancestorIds: string[] = [];
        let parentId = selectedGroupId != null ? groupsById.get(selectedGroupId)?.parent?.id : undefined;
        while (parentId != null && !ancestorIds.includes(parentId)) {
            ancestorIds.push(parentId);
            parentId = groupsById.get(parentId)?.parent?.id;
        }
        setExpandedGroupIds((prev) => {
            const next = quickFilter ? [...prev, ...ancestorIds.filter((id) => !prev.includes(id))] : ancestorIds;
            return next.length === prev.length && next.every((id) => prev.includes(id)) ? prev : next;
        });
    }, [selectedGroupId, groupsById, quickFilter]);

    // Amb el filtre ràpid actiu es desplega tot el que coincideix.
    React.useEffect(() => {
        if (!quickFilter) {
            return;
        }
        const parentIds = Array.from(childrenByParent.keys()).filter((id): id is string => id != null);
        setExpandedGroupIds((prev) =>
            parentIds.every((id) => prev.includes(id)) ? prev : [...prev, ...parentIds.filter((id) => !prev.includes(id))]
        );
    }, [quickFilter, childrenByParent]);

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
            <ScreenFrame
                icon={icons.propietat}
                title={t('page.propietats.title')}
                // El cercador i el botó de sincronitzar van a la barra de títol, com la barra d'eines de les graelles
                actions={
                    <>
                        <QuickFilter onChange={setQuickFilter} />
                        <ToolbarButton
                            title={t('page.propietats.accio.sync.label')}
                            icon={icons.sincronitza}
                            color="primary"
                            disabled={sincronitzant || !isConfigReady}
                            onClick={sincronitza}
                        >
                            {t('page.propietats.accio.sync.label')}
                        </ToolbarButton>
                    </>
                }
            >
                {senseResultats ? (
                    <Box sx={{ textAlign: 'center', px: 2, py: 6 }}>
                        <Icon fontSize="large" color="disabled">
                            block
                        </Icon>
                        <Typography variant="h5" color="text.secondary">
                            {t('page.propietats.empty')}
                        </Typography>
                    </Box>
                ) : (
                    // Arbre i detall ocupen l'alçada que sobra a la pantalla (cada un fa el seu
                    // scroll per dins) en lloc de tenir una alçada fixa en vh.
                    <Box
                        sx={{
                            display: 'flex',
                            gap: MAIN_GAP + 'px',
                            flex: '1 1 0',
                            minHeight: SCREEN_GRID_MIN_HEIGHT + 'px',
                        }}
                    >
                        <Box
                            sx={(theme) => ({
                                flex: '0 0 25%',
                                overflow: 'auto',
                                // Al clar la vora de sempre; als foscos, el divisor del tema (el gris clar
                                // fix es veia com un marc blanc)
                                border: '1px solid',
                                borderColor: theme.palette.mode === 'dark' ? theme.palette.divider : '#e0e0e0',
                                borderRadius: 1,
                            })}
                        >
                            <SimpleTreeView
                                selectedItems={selectedGroupId ?? null}
                                onSelectedItemsChange={(_event, id) => {
                                    if (id && !isContainer(id)) {
                                        setSelectedGroupId(id);
                                    }
                                }}
                                expandedItems={expandedGroupIds}
                                onExpandedItemsChange={(_event, ids) => setExpandedGroupIds(ids)}
                                sx={(theme) => ({
                                    '& .MuiTreeItem-content': { paddingY: 1 },
                                    // Als temes foscos el grup seleccionat s'ha de veure: fons atenuat i
                                    // vora esquerra com l'entrada seleccionada del menú
                                    ...(theme.palette.mode === 'dark' && {
                                        '& .MuiTreeItem-content.Mui-selected, & .MuiTreeItem-content.Mui-selected:hover, & .MuiTreeItem-content.Mui-selected.Mui-focused':
                                            {
                                                backgroundColor: alpha(theme.palette.primary.main, 0.35),
                                                boxShadow: `inset 4px 0 0 ${accentColor(theme)}`,
                                                fontWeight: 600,
                                            },
                                    }),
                                })}
                            >
                                <GroupTreeItems childrenByParent={childrenByParent} />
                            </SimpleTreeView>
                        </Box>
                        <Box sx={{ flex: '1 1 0', minWidth: 0 }}>
                            <PropietatsProps
                                groupId={selectedGroupId}
                                grups={filteredGroups}
                                configs={filteredProps}
                                highlight={quickFilter}
                                refreshKey={refreshKey}
                                onSaved={alDesar}
                            />
                        </Box>
                    </Box>
                )}
            </ScreenFrame>
        </GridPage>
    );
};

export default Propietats;
