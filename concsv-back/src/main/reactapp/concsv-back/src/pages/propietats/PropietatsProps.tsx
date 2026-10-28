import React from 'react';
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
    /** Tots els grups visibles (per localitzar el grup). */
    grups: PropietatGrup[];
    /** Propietats ja filtrades. */
    configs: PropietatItem[];
    /** Text del filtre ràpid, que es ressalta a les files. */
    highlight?: string;
    /** Canvia quan s'ha de tornar a carregar tot (p. ex. després de sincronitzar). */
    refreshKey?: number;
    /** Es crida amb la propietat desada. */
    onSaved?: (item: PropietatItem) => void;
};

/**
 * Propietats del grup seleccionat: una fila per cada propietat del grup. Els subgrups es naveguen
 * des de l'arbre del menú. Ocupa tota l'alçada del contenidor, amb la capçalera fixa i només el
 * contingut amb scroll.
 */
export const PropietatsProps: React.FC<PropietatsPropsProps> = ({
    groupId,
    grups,
    configs,
    highlight,
    refreshKey = 0,
    onSaved,
}) => {
    const { t } = useTranslation();

    const group = React.useMemo(() => grups?.find((g) => g.id === groupId), [groupId, grups]);
    const confProps = configs?.filter((p) => p.group?.id === groupId);

    return (
        <Load value={group} noEffect>
            <DetailCard
                title={group?.description}
                variant={'h6'}
                headerProps={{ color: 'white', backgroundColor: 'primary.main' }}
                gridProps={{ sx: { height: '100%' } }}
                cardProps={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                contentProps={{ flex: 1, minHeight: 0, overflow: 'auto' }}
            >
                <Grid size={12}>
                    <List component={Paper}>
                        {confProps?.length ? (
                            confProps.map((c) => (
                                <PropietatsForm
                                    key={`item-${refreshKey}-${c.id}`}
                                    id={c.id}
                                    highlight={highlight}
                                    onSaved={onSaved}
                                />
                            ))
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
