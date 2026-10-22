import React from 'react';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Typography from '@mui/material/Typography';
import { useResourceApiService } from 'reactlib';
import { useConcsvContext, ROLE_SUPER } from './ConcsvContext';

// Icona de distintiu sobre l'avatar de l'usuari segons el rol actual. S'usa a headerAuthBadgeIcon
// de MuiBaseApp.
export const getRolBadgeIcon = (rolActual?: string): string | undefined =>
    rolActual === ROLE_SUPER ? 'shield' : undefined;

/**
 * Desa una preferència al perfil de l'usuari (csv_usuari) perquè el proper inici de sessió hi
 * torni. Només es crida des del selector, que és l'acció explícita de l'usuari: les altres
 * pestanyes reben el canvi pel BroadcastChannel i no l'han de reescriure (dues escriptures
 * simultànies xocarien amb el control de versió).
 */
const useDesarPreferencia = () => {
    const { currentUser, setCurrentUser } = useConcsvContext();
    const { isReady: usuariApiIsReady, patch: usuariApiPatch } = useResourceApiService('usuariResource');
    return (camp: string, valor: any) => {
        if (!usuariApiIsReady || currentUser?.id == null) {
            return;
        }
        usuariApiPatch(currentUser.id, { data: { [camp]: valor } })
            .then(() => setCurrentUser({ ...currentUser, [camp]: valor }))
            .catch((error: any) => console.error("No s'ha pogut desar la preferència " + camp, error));
    };
};

// Selector de l'entitat de treball. El superusuari no en té (veu les dades de totes les
// entitats). Amb una única entitat accessible només es mostra l'etiqueta, sense desplegable.
export const EntitatSelector: React.FC = () => {
    const { entitatsAvailable, currentEntitatId, currentRole, setCurrentEntitatId } = useConcsvContext();
    const desarPreferencia = useDesarPreferencia();
    const entitats = entitatsAvailable ?? [];
    if (entitats.length === 0 || currentRole === ROLE_SUPER) {
        return null;
    }
    if (entitats.length === 1) {
        return (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, px: 1 }}>
                <Icon fontSize="small">domain</Icon>
                <Typography variant="body2">{entitats[0].nom}</Typography>
            </Box>
        );
    }
    return (
        <Select
            size="small"
            value={currentEntitatId ?? ''}
            onChange={(event) => {
                const entitatId = Number(event.target.value);
                setCurrentEntitatId(entitatId);
                desarPreferencia('entitatActualId', entitatId);
            }}
            renderValue={(value) => (
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                    <Icon fontSize="small">domain</Icon>
                    {entitats.find((entitat) => entitat.id === value)?.nom ?? ''}
                </Box>
            )}
            sx={{ minWidth: 140 }}
        >
            {entitats.map((entitat) => (
                <MenuItem key={entitat.id} value={entitat.id}>
                    {entitat.nom}
                </MenuItem>
            ))}
        </Select>
    );
};
