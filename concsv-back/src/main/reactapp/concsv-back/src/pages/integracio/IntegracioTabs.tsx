import React from 'react';
import Tab from '@mui/material/Tab';
import Tabs from '@mui/material/Tabs';
import { useResourceApiService } from 'reactlib';
import { StyledBadge } from '../../components/StyledBadge';

const INTEGRACIO_RESOURCE = 'monitorIntegracioResource';
const ACTION_PIPELLES = 'PIPELLES';

/** Pestanya d'una integració, tal com la serveix l'acció PIPELLES del backend. */
export interface IntegracioPipella {
    codi: string;
    nom: string;
    errors?: number | null;
}

/** Resposta de l'acció PIPELLES */
interface PipellesResult {
    pipelles: IntegracioPipella[];
}

/**
 * Pestanyes del monitor d'integracions: les serveix el backend (una per integració, amb el nom
 * traduït i el nombre de peticions amb error, que es mostra com a badge). Es carreguen una sola
 * vegada, en entrar a la pantalla. La pestanya seleccionada s'afegeix com "named query" del
 * llistat, que el backend tradueix a una condició sobre el camp `codi`.
 *
 * `ready` és fals mentre es carreguen. Si la càrrega falla, `error` té el missatge (buit si el
 * servidor no n'ha donat cap) i no hi ha pestanyes: la pantalla no ha de mostrar el llistat.
 */
export const useIntegracioTabs = () => {
    const { isReady, artifactAction } = useResourceApiService(INTEGRACIO_RESOURCE);
    const [pipelles, setPipelles] = React.useState<IntegracioPipella[]>([]);
    const [value, setValue] = React.useState<string>();
    const [ready, setReady] = React.useState(false);
    const [error, setError] = React.useState<string>();
    const iniciat = React.useRef(false);

    React.useEffect(() => {
        if (!isReady || iniciat.current) {
            return;
        }
        iniciat.current = true;
        artifactAction(undefined, { code: ACTION_PIPELLES })
            .then((result: PipellesResult) => {
                const llista = result?.pipelles ?? [];
                setPipelles(llista);
                setValue((actual) => actual ?? llista[0]?.codi);
            })
            .catch((e: { message?: string }) => setError(e?.message ?? ''))
            .finally(() => setReady(true));
    }, [isReady, artifactAction]);

    const handleChange = (_event: React.SyntheticEvent, newValue: string) => {
        setValue(newValue);
    };

    const tabElement = (
        <Tabs value={value ?? false} onChange={handleChange} variant="scrollable" sx={{ px: 1 }}>
            {pipelles.map((pipella) => (
                <Tab
                    key={pipella.codi}
                    value={pipella.codi}
                    label={
                        <StyledBadge badgeContent={pipella.errors} badgecolor="error" max={999}>
                            {pipella.nom}
                        </StyledBadge>
                    }
                    sx={{ textTransform: 'none' }}
                />
            ))}
        </Tabs>
    );

    return { value, ready, error, tabElement };
};

export default useIntegracioTabs;
