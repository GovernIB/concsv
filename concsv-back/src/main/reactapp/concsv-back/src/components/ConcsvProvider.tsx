import React from 'react';
import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Icon from '@mui/material/Icon';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import { useAuthContext, useResourceApiContext, useResourceApiService } from 'reactlib';
import { ConcsvContext, ROLE_SUPER } from './ConcsvContext';
import { useSessioUsuari } from './ConcsvAuthProvider';
import { idiomaAplicacio } from '../util/idioma';

export const concsvChannel = new BroadcastChannel('concsv');
type CurrentSession = Readonly<{
    entitatId?: number;
}>;

/**
 * Entitat de treball de la pestanya, sincronitzada amb les altres pestanyes de l'aplicació.
 *
 * Només es difonen els canvis que fa l'usuari des del selector (`publishSession`). Els passos
 * d'inicialització de la pestanya (`setSession`) són locals: si es difonguessin, una pestanya que
 * arrenca (pestanya nova, F5) enviaria `entitatId: undefined` a les altres, que mostrarien la
 * pantalla de càrrega i desmuntarien tota l'aplicació, i a més els imposaria la seva entitat
 * inicial.
 */
const useBroadcastSession = () => {

    const [session, setSessionState] = React.useState<CurrentSession>({});

    /** Canvi local de la pestanya, sense difondre'l. */
    const setSession = React.useCallback((changes: Partial<CurrentSession>) => {
        setSessionState((previous) => ({ ...previous, ...changes }));
    }, []);

    /**
     * Canvi fet per l'usuari: s'aplica i es difon a les altres pestanyes. El missatge s'envia
     * fora de l'updater de l'estat, que ha de ser pur (StrictMode l'executa dues vegades).
     */
    const publishSession = React.useCallback((next: CurrentSession) => {
        setSessionState(next);
        concsvChannel.postMessage(next);
    }, []);

    React.useEffect(() => {

        const listener = ({ data }: MessageEvent<CurrentSession>) => {
            if (!data) {
                return;
            }
            // Si no canvia res es conserva l'objecte anterior perquè React no torni a pintar.
            setSessionState((previous) =>
                previous.entitatId === data.entitatId ? previous : { entitatId: data.entitatId }
            );
        };

        concsvChannel.addEventListener("message", listener);

        return () =>
            concsvChannel.removeEventListener("message", listener);

    }, []);

    return {
        session,
        setSession,
        publishSession,
    };

};

type BroadcastSession = ReturnType<typeof useBroadcastSession>;

const useSessionStorage = (...keyParts: any[]) => {

    const key = keyParts.map((p) => (typeof p === 'object' && p !== null ? JSON.stringify(p) : String(p))).join('|');
    const getValue = () => sessionStorage.getItem(key);
    const setValue = (value: string | null) => {
        if (value == null) {
            sessionStorage.removeItem(key);
            return;
        }
        sessionStorage.setItem(key, value);
    };
    return {getValue, setValue,};
};

const useCurrentUser = () => {

    const {isReady: apiIsReady, find: apiFind,} = useResourceApiService('usuariResource');
    const [currentUser, setCurrentUser] = React.useState<any>();
    React.useEffect(() => {
        if (!apiIsReady) {
            return;
        }
        void apiFind({ unpaged: true }).then((response) => {
            if (response.rows.length) {
                setCurrentUser(response.rows[0]);
            }
        });
    }, [apiIsReady]);
    return { currentUser, setCurrentUser };
};

/**
 * Rol amb què opera la interfície. Només hi poden entrar els usuaris amb CSV_SUPER (a la resta
 * ConcsvProvider els mostra la pantalla de sense permisos), així que és sempre aquest i no hi ha
 * selector de rol. Es continua enviant a la capçalera X-App-Role perquè RolSeleccionatFilter
 * restringeixi les autoritats de cada petició a aquest rol, sense el rol base "tothom" que el
 * backend concedeix a tot usuari autenticat.
 */
const useCurrentRole = () => {

    const { usuari: sessioUsuari } = useSessioUsuari();
    const { httpHeaders: apiHttpHeaders, setHttpHeaders: apiSetHttpHeaders } = useResourceApiContext();
    const currentRole = ROLE_SUPER;
    React.useEffect(() => {
        apiSetHttpHeaders([{ 'X-App-Role': currentRole }]);
    }, []);
    const currentRoleFromHttpHeader = apiHttpHeaders?.find((h) => 'X-App-Role' in h)?.['X-App-Role'];
    return {
        currentUserId: sessioUsuari?.codi,
        currentRole,
        currentRoleReady: currentRole === currentRoleFromHttpHeader,
    };
};

const useCurrentEntitat = (
    broadcast: BroadcastSession,
    currentUserId: string | undefined,
    entitatActualId: number | undefined,
    currentRole: string | undefined,
    currentRoleReady: boolean
) => {

    const { httpHeaders: apiHttpHeaders, setHttpHeaders: apiSetHttpHeaders } = useResourceApiContext();
    const {isReady: apiIsReady, find: apiFind, getOne: apiGetOne} = useResourceApiService('entitatResource');
    // Les entitats es guarden amb el rol per al qual s'han carregat. En canviar de rol, la llista
    // de l'anterior es conserva fins que l'efecte que la recarrega s'executa; sense aquesta
    // comprovació hi havia un render amb el rol nou, la llista vella i cap entitat que es donava
    // per bo, i l'aplicació s'arrencava un instant sense entitat.
    const [entitatsCarregades, setEntitatsCarregades] = React.useState<{ rol: string; entitats: any[] }>();
    const entitatsAvailable = entitatsCarregades?.rol === currentRole ? entitatsCarregades?.entitats : undefined;
    const [currentEntitatLoading, setCurrentEntitatLoading] = React.useState<boolean>();
    const [currentEntitat, setCurrentEntitat] = React.useState<any>();
    const { getValue: sessionSessionGetValue, setValue: sessionSessionSetValue } = useSessionStorage(currentUserId, 'currentSession');
    const {
        session,
        setSession,
        publishSession
    } = broadcast;

    const currentEntitatId = session.entitatId;

    // Canvi d'entitat des del selector: es difon a les altres pestanyes.
    const setCurrentEntitatId = (id?: number) => publishSession({ entitatId: id });
    // Inicialització de l'entitat de la pestanya: local.
    const setCurrentEntitatIdLocal = (id?: number) => setSession({ entitatId: id });

    React.useEffect(() => {
        if (!apiIsReady || !currentRoleReady || currentRole == null) {
            return;
        }
        setEntitatsCarregades(undefined);
        setCurrentEntitat(undefined);
        setCurrentEntitatIdLocal(undefined);

        if (currentRole === ROLE_SUPER) {
            setEntitatsCarregades({ rol: currentRole, entitats: [] });
            return;
        }
        apiFind({ unpaged: true }).then((response) => {
            const entitatsAvailable = response.rows;
            setEntitatsCarregades({ rol: currentRole, entitats: entitatsAvailable });

            // Entitat de treball inicial, per aquest ordre: la de la pestanya actual
            // (sessionStorage), la darrera amb què va operar l'usuari (csv_usuari.entitat_actual_id,
            // que desa el selector de la capçalera) i la primera accessible.
            const storedSession = sessionSessionGetValue();
            const parsedSession = storedSession ? JSON.parse(storedSession) : {};
            const idsAccessibles = entitatsAvailable.map((e) => e.id);
            const esAccessible = (id?: number) => id != null && idsAccessibles.includes(id);
            const entitatInicial =
                [parsedSession.e, entitatActualId].find(esAccessible) ?? idsAccessibles[0];
            // Sense comprovar currentEntitatId: l'efecte acaba de posar-lo a undefined i el valor
            // que es veuria des d'aquí seria el del rol anterior, que impediria fixar el nou.
            if (entitatInicial != null) {
                setCurrentEntitatIdLocal(entitatInicial);
            }
        });
    }, [apiIsReady, currentRoleReady, currentRole]);

    React.useEffect(() => {
        if (currentRole == null || currentRole === ROLE_SUPER || currentEntitatId == null) {
            return;
        }

        const sessionJson = JSON.stringify({ e: currentEntitatId });
        sessionSessionSetValue(sessionJson);

        apiSetHttpHeaders([
            { "X-App-Role": currentRole },
            { "X-App-Session": sessionJson },
        ]);
    }, [currentRole, currentEntitatId]);

    React.useEffect(() => {
        if (!apiIsReady || currentEntitatId == null || entitatsAvailable == null) {
            return;
        }
        const entitatExisteix = entitatsAvailable.some(e => e.id === currentEntitatId);
        if (!entitatExisteix) {
            return;
        }
        setCurrentEntitatLoading(true);
        apiGetOne(currentEntitatId)
        .then(setCurrentEntitat)
        .finally(() => setCurrentEntitatLoading(false));

    }, [apiIsReady, currentEntitatId, entitatsAvailable,]);

    const currentSessionFromHttpHeader = apiHttpHeaders?.find((h) => 'X-App-Session' in h)?.['X-App-Session'];
    const currentEntitatIdFromHttpHeader = currentSessionFromHttpHeader != null ? JSON.parse(currentSessionFromHttpHeader).e : undefined;
    const entitatIdHttpHeaderInitialized = currentRole === ROLE_SUPER
        || (currentEntitatId == null && currentEntitatIdFromHttpHeader == null)
        || currentEntitatId === currentEntitatIdFromHttpHeader;
    const currentEntitatReady = apiIsReady && entitatsAvailable != null && entitatIdHttpHeaderInitialized;
    return {
        currentEntitatId,
        currentEntitatReady,
        currentEntitat,
        currentEntitatLoading,
        entitatsAvailable,
        setCurrentEntitatId,
    };
};

const ConcsvProviderLoading: React.FC = () => {

    const { t } = useTranslation();
    return (
        <Box sx={{display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100vh',}}>
            <CircularProgress size={70} />
            <Typography sx={{ mt: 1 }}>{t('app.loading')}</Typography>
        </Box>
    );
};

/**
 * Aplica a l'API (Accept-Language) l'idioma del perfil de l'usuari tan bon punt se'l coneix, i diu
 * si ja hi és aplicat.
 *
 * BaseApp també el passa a base-react, però des de dins de l'aplicació, un cop pintada. Si fos
 * aquell el primer a aplicar-lo, l'índex de l'API es tornaria a carregar amb l'aplicació ja
 * muntada, i l'aplicació es desmuntaria i es tornaria a muntar. Aplicant-lo aquí i esperant-lo
 * abans de donar-se per llest, quan base-react el posa ja té el mateix valor i no recarrega res.
 */
const useIdiomaApi = (currentUser: { idioma?: string } | undefined) => {

    const { currentLanguage: apiCurrentLanguage, setCurrentLanguage: apiSetCurrentLanguage } = useResourceApiContext();
    const idioma = currentUser != null ? idiomaAplicacio(currentUser.idioma) : undefined;
    React.useEffect(() => {
        if (idioma != null && idioma !== apiCurrentLanguage) {
            apiSetCurrentLanguage(idioma);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [idioma]);
    return idioma != null && idioma === apiCurrentLanguage;
};

/** Pantalla per als usuaris autenticats que no tenen el rol CSV_SUPER. */
const ConcsvProviderSensePermisos: React.FC<{ usuari: string }> = ({ usuari }) => {

    const { t } = useTranslation();
    const { signOut } = useAuthContext();
    return (
        <Box sx={{display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', height: '100vh', gap: 2, px: 2, textAlign: 'center',}}>
            <Icon color="error" sx={{ fontSize: 70 }}>lock</Icon>
            <Typography variant="h5">{t('app.sensePermisos.titol')}</Typography>
            <Typography>{t('app.sensePermisos.missatge', { usuari })}</Typography>
            <Button variant="contained" onClick={() => signOut?.()}>
                {t('app.sensePermisos.tancarSessio')}
            </Button>
        </Box>
    );
};

const ConcsvProviderAutoritzat: React.FC<React.PropsWithChildren> = ({ children }) => {

    const { offline: apiOffline } = useResourceApiContext();
    const broadcast = useBroadcastSession();
    const { currentUser, setCurrentUser } = useCurrentUser();
    const { currentUserId, currentRole, currentRoleReady } = useCurrentRole();
    const {
        currentEntitatId,
        currentEntitatReady,
        currentEntitat,
        currentEntitatLoading,
        entitatsAvailable,
        setCurrentEntitatId,
    } = useCurrentEntitat(
        broadcast,
        currentUserId,
        currentUser?.entitatActualId,
        currentRole,
        currentRoleReady
    );
    const idiomaApiReady = useIdiomaApi(currentUser);
    const isReady = apiOffline || (currentRoleReady && currentEntitatReady && currentUser != null && idiomaApiReady);
    const contextValue = {
        isReady,
        currentUser,
        setCurrentUser,
        currentRole,
        entitatsAvailable,
        currentEntitatId,
        setCurrentEntitatId,
        currentEntitat,
        currentEntitatLoading,
    };
    return (
        <ConcsvContext.Provider value={contextValue}>
            {isReady ? children : <ConcsvProviderLoading />}
        </ConcsvContext.Provider>
    );
};

export const ConcsvProvider: React.FC<React.PropsWithChildren> = ({ children }) => {

    const { usuari: sessioUsuari } = useSessioUsuari();
    // Només poden entrar a l'aplicació els usuaris amb el rol CSV_SUPER. Es comprova abans de
    // muntar ConcsvProviderAutoritzat perquè als altres no se'ls faci cap crida a l'API.
    if (sessioUsuari != null && !sessioUsuari.rols.includes(ROLE_SUPER)) {
        return <ConcsvProviderSensePermisos usuari={sessioUsuari.nom ?? sessioUsuari.codi} />;
    }
    return <ConcsvProviderAutoritzat>{children}</ConcsvProviderAutoritzat>;
};

export default ConcsvProvider;
