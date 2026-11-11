import React from 'react';
import { useTranslation } from 'react-i18next';
import { useLocation } from 'react-router-dom';
import { envVar } from '@programari-limit/base-react';
import { envVars } from '../main';
import { AvisosContext, getAvisNivell, type Avis, type AvisNivell, type AvisosState } from './AvisosContext';

type AvisosResponse = {
    _embedded?: {
        avisos?: (Avis & { actiu?: boolean })[];
    };
};

/**
 * Proveïdor dels avisos actius i dels que l'usuari ha minimitzat. Ha d'envoltar la capçalera (chips)
 * i les pàgines (banner), que són a llocs diferents de l'arbre; per això és a `BaseCaibApp`, per
 * fora de `MuiBaseApp`.
 *
 * Consulta els avisos al servidor en carregar l'aplicació, en canviar de pàgina, en canviar
 * d'idioma i en tornar a la pestanya. Mentre es consulta, o si la consulta falla, es conserva la
 * llista anterior: només una resposta correcta pot fer que un avís s'oblidi.
 */
export const AvisosProvider: React.FC<React.PropsWithChildren> = ({ children }) => {
    const { i18n } = useTranslation();
    const { pathname } = useLocation();
    const language = i18n.language;
    const [avisos, setAvisos] = React.useState<Avis[]>([]);
    const [idsMinimitzats, setIdsMinimitzats] = React.useState<ReadonlySet<number>>(new Set());

    const carrega = React.useCallback(async (signal?: AbortSignal) => {
        try {
            const baseUrl = envVar('VITE_API_URL', envVars) || '/api';
            const response = await fetch(`${baseUrl}/avisos/actius?lang=${language}`, { signal });
            if (!response.ok) {
                return;
            }
            const data: AvisosResponse = await response.json();
            const actius = (data._embedded?.avisos ?? []).filter((avis) => avis.actiu !== false);
            // Només es canvia l'estat si la llista ha canviat, per no repintar banner i chips per res.
            setAvisos((previous) => (JSON.stringify(previous) === JSON.stringify(actius) ? previous : actius));
        } catch (error) {
            if ((error as Error)?.name !== 'AbortError') {
                console.error("Error carregant els avisos", error);
            }
        }
    }, [language]);

    React.useEffect(() => {
        const controller = new AbortController();
        carrega(controller.signal);
        return () => controller.abort();
    }, [carrega, pathname]);

    React.useEffect(() => {
        const onVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                carrega();
            }
        };
        document.addEventListener('visibilitychange', onVisibilityChange);
        return () => document.removeEventListener('visibilitychange', onVisibilityChange);
    }, [carrega]);

    // Oblida els avisos que han deixat de ser actius.
    React.useEffect(() => {
        setIdsMinimitzats((previous) => {
            const actius = new Set(avisos.map((avis) => avis.id));
            const filtrats = [...previous].filter((id) => actius.has(id));
            return filtrats.length === previous.size ? previous : new Set(filtrats);
        });
    }, [avisos]);

    const value = React.useMemo<AvisosState>(() => {
        const delNivell = (nivell: AvisNivell) => avisos.filter((avis) => getAvisNivell(avis) === nivell);

        return {
            visibles: avisos.filter((avis) => !idsMinimitzats.has(avis.id)),
            total: (nivell) => delNivell(nivell).length,
            minimitzats: (nivell) => delNivell(nivell).filter((avis) => idsMinimitzats.has(avis.id)).length,
            minimitza: (id) => setIdsMinimitzats((previous) => new Set(previous).add(id)),
            restaura: (nivell) => {
                const ids = delNivell(nivell).map((avis) => avis.id);
                setIdsMinimitzats((previous) => {
                    if (!ids.some((id) => previous.has(id))) {
                        return previous;
                    }
                    const next = new Set(previous);
                    ids.forEach((id) => next.delete(id));

                    return next;
                });
            },
        };
    }, [avisos, idsMinimitzats]);

    return <AvisosContext.Provider value={value}>{children}</AvisosContext.Provider>;
};

export default AvisosProvider;
