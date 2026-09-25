import React from 'react';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Collapse from '@mui/material/Collapse';
import Icon from '@mui/material/Icon';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import MenuItem from '@mui/material/MenuItem';
import Select from '@mui/material/Select';
import Typography from '@mui/material/Typography';
import { useNavigate } from 'react-router-dom';
import { useResourceApiService } from 'reactlib';
import { useConcsvContext, ROLE_SUPER } from './ConcsvContext';
import { rutaInicialPerRol } from '../util/pantalles';

// Icona de distintiu sobre l'avatar de l'usuari segons el rol actual (cap distintiu per al rol
// base). S'usa a headerAuthBadgeIcon de MuiBaseApp.
export const getRolBadgeIcon = (rolActual?: string): string | undefined =>
    rolActual === ROLE_SUPER ? 'shield' : undefined;

/**
 * Desa una preferència al perfil de l'usuari (csv_usuari) perquè el proper inici de sessió hi
 * torni. Només es crida des dels selectors, que són l'acció explícita de l'usuari: les altres
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

// Pensat per viure dins el menú desplegable de l'usuari (headerAdditionalAuthComponents): amb un
// únic rol disponible només mostra l'etiqueta; amb més d'un, un ítem plegable que en desplegar-se
// mostra la resta d'opcions (l'actual remarcada amb `selected`).
export const RolSelector: React.FC = () => {
    const { t } = useTranslation();
    const { rolesAvailable, currentRole, setCurrentRole } = useConcsvContext();
    const desarPreferencia = useDesarPreferencia();
    const navigate = useNavigate();
    const [expanded, setExpanded] = React.useState(false);
    const rolsDisponibles = rolesAvailable ?? [];
    const label = (rol: string) => t(`component.EntitatRolSelector.rol.${rol}`, rol);
    if (rolsDisponibles.length === 0 || !currentRole) {
        return null;
    }
    if (rolsDisponibles.length === 1) {
        return (
            <MenuItem
                disableRipple
                sx={{ '&.MuiButtonBase-root:hover': { bgcolor: 'transparent', cursor: 'default' } }}
            >
                <ListItemIcon>
                    <Icon fontSize="small">badge</Icon>
                </ListItemIcon>
                <ListItemText>{label(currentRole)}</ListItemText>
            </MenuItem>
        );
    }
    return (
        <>
            <MenuItem onClick={() => setExpanded((prev) => !prev)}>
                <ListItemIcon>
                    <Icon fontSize="small">badge</Icon>
                </ListItemIcon>
                <ListItemText>{label(currentRole)}</ListItemText>
                <Icon fontSize="small">{expanded ? 'expand_less' : 'expand_more'}</Icon>
            </MenuItem>
            <Collapse in={expanded} timeout="auto" unmountOnExit>
                {rolsDisponibles.map((rol) => (
                    <MenuItem
                        key={rol}
                        selected={rol === currentRole}
                        sx={{ pl: 4 }}
                        onClick={() => {
                            setCurrentRole(rol);
                            setExpanded(false);
                            desarPreferencia('rolActual', rol);
                            // Amb el rol nou la pantalla actual pot quedar prohibida (p. ex. passar
                            // a "tothom" des d'un manteniment): s'hi va a la d'inici del rol.
                            navigate(rutaInicialPerRol(rol));
                        }}
                    >
                        <ListItemText>{label(rol)}</ListItemText>
                    </MenuItem>
                ))}
            </Collapse>
        </>
    );
};
