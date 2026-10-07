import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Grid } from '@mui/material';
import Box from '@mui/material/Box';
import Paper from '@mui/material/Paper';
import List from '@mui/material/List';
import Icon from '@mui/material/Icon';
import Typography from '@mui/material/Typography';
import { DetailCard } from '../../components/CardData';
import Load from '../../components/Load';
import { PropietatsForm } from './PropietatsForm';
import type { PropietatGrup, PropietatItem } from './PropietatsTypes';

type PropietatsPropsProps = {
    /** Identificador del grup a mostrar. */
    groupId?: string;
    /** Tots els grups (per localitzar el grup i els seus subgrups). */
    grups: PropietatGrup[];
    /** Propietats ja filtrades. */
    configs: PropietatItem[];
    /** Text del filtre ràpid, que es ressalta a les files. */
    highlight?: string;
    /** Canvia quan s'ha de tornar a carregar tot (p. ex. després de sincronitzar). */
    refreshKey?: number;
    /** Es crida amb la propietat desada. */
    onSaved?: (item: PropietatItem) => void;
    /**
     * Grup de nivell superior: ocupa tota l'alçada del contenidor, amb la capçalera fixa i només
     * el contingut amb scroll. Els subgrups no ho són i flueixen dins el contingut del pare.
     */
    root?: boolean;
};

/**
 * Propietats d'un grup: una fila per cada propietat del grup i, a sota, els seus subgrups (cada un
 * amb el mateix aspecte).
 */
export const PropietatsProps: React.FC<PropietatsPropsProps> = ({
    groupId,
    grups,
    configs,
    highlight,
    refreshKey = 0,
    onSaved,
    root = false,
}) => {
    const { t } = useTranslation();

    const group = useMemo(() => grups?.find((g) => g.id === groupId), [groupId, grups]);
    const confProps = configs?.filter((p) => p.group?.id === groupId);
    const children = grups?.filter((p) => p.parent?.id === groupId);

    return (
        <Load value={group} noEffect>
            <DetailCard
                title={group?.description}
                variant={'h6'}
                headerProps={{ color: 'white', backgroundColor: 'primary.main' }}
                {...(root && {
                    gridProps: { sx: { height: '100%' } },
                    cardProps: { height: '100%', display: 'flex', flexDirection: 'column' },
                    contentProps: { flex: 1, minHeight: 0, overflow: 'auto' },
                })}
            >
                <Grid size={12}>
                    <List component={Paper}>
                        {confProps?.length || children?.length ? (
                            <>
                                {confProps?.map((c) => (
                                    <PropietatsForm
                                        key={`item-${refreshKey}-${c.id}`}
                                        id={c.id}
                                        highlight={highlight}
                                        onSaved={onSaved}
                                    />
                                ))}
                                {children?.map((c) => (
                                    <Box key={c.key} sx={{ px: 1 }}>
                                        <PropietatsProps
                                            groupId={c.id}
                                            grups={grups}
                                            configs={configs}
                                            highlight={highlight}
                                            refreshKey={refreshKey}
                                            onSaved={onSaved}
                                        />
                                    </Box>
                                ))}
                            </>
                        ) : (
                            <Box sx={{ width: '100%', textAlign: 'center', px: 2, py: 4 }}>
                                <Icon fontSize="large" color="disabled">
                                    block
                                </Icon>
                                <Typography variant="h5" color="text.secondary">
                                    {t('page.propietats.empty')}
                                </Typography>
                            </Box>
                        )}
                    </List>
                </Grid>
            </DetailCard>
        </Load>
    );
};

export default PropietatsProps;
