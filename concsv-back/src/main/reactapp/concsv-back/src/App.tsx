import { useTranslation } from 'react-i18next';
import { Outlet } from 'react-router-dom';
import { useTheme } from '@mui/material/styles';
import { envVar, ResourceApiProvider } from 'reactlib';
import { BaseApp } from './components/BaseApp';
import DrassanaFooter from './components/DrassanaFooter';
import goibLogoLight from './assets/goib_logo_light.svg';
import goibLogoDark from './assets/goib_logo_dark.svg';
import concsvLogo from './assets/CON_DRA_COL.png';
import { UserPreferencesProvider, useUserPreferences } from './components/UserProfile';
import { TemaProvider } from './components/TemaProvider';
import { ConcsvProvider } from './components/ConcsvProvider';
import { ConcsvAuthProvider } from './components/ConcsvAuthProvider';
import { useConcsvContext } from './components/ConcsvContext';
import { filtrarEntradesMenu, type MenuEntryAmbPantalla } from './util/pantalles';
import { icons } from './util/icons';
import {
    APPBAR_PADDING_LEFT,
    FOOTER_HEIGHT,
    HEADER_HEIGHT,
    LOGO_BOX_LEFT,
    LOGO_HEIGHT,
    LOGO_SEPARATOR_GAP,
    MENU_WIDTH,
} from './util/layout';
import { SessionStorageProvider } from './components/SessionStorageContext';
import TitolPagina from './components/TitolPagina';
import {SnackbarProvider} from "notistack";

export const envVars = {
    VITE_API_URL: import.meta.env.VITE_API_URL,
    VITE_API_PUBLIC_URL: import.meta.env.VITE_API_PUBLIC_URL,
    VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
    VITE_API_SUFFIX: import.meta.env.VITE_API_SUFFIX,
    VITE_APP_VERSION: import.meta.env.VITE_APP_VERSION,
};

export const getEnvApiUrl = () => {
    const envApiPublicUrl = envVar('VITE_API_PUBLIC_URL', envVars);
    const envApiUrl = envVar('VITE_API_URL', envVars);
    if (envApiPublicUrl || envApiUrl) {
        return envApiPublicUrl ?? envApiUrl;
    } else {
        const envApiBaseUrl = envVar('VITE_API_BASE_URL', envVars);
        const envApiSuffix = envVar('VITE_API_SUFFIX', envVars) ?? '/api';
        if (envApiBaseUrl) {
            return envApiBaseUrl + envApiSuffix;
        } else {
            // Per defecte l'API penja del mateix context que l'SPA: /concsvback/reactapp/ -> /concsvback/api/
            // (amb la barra final: els recursos es concatenen directament a aquesta URL).
            const contextPath = import.meta.env.BASE_URL.replace(/reactapp\/?$/, '').replace(/\/$/, '');
            return window.location.origin + contextPath + envApiSuffix + '/';
        }
    }
};

const version = import.meta.env.VITE_APP_VERSION ?? '0.0.0';

// Les mides de la capçalera, del menú i del peu són a util/layout.ts: MENU_WIDTH és l'amplada del
// menú lateral obert, APPBAR_PADDING_LEFT desplaça el botó de menú fins a la columna de les icones
// del menú, i LOGO_BOX_LEFT és on comença la caixa del logo amb aquest padding.

const InnerApp: React.FC = () => {
    const { t } = useTranslation();
    const theme = useTheme();
    const mode = theme.palette.mode;

    const { currentRole } = useConcsvContext();
    // La pantalla de cada entrada determina a quins rols es mostra (veure PANTALLA_ROLS a
    // util/pantalles.ts): el menú i les guardes de ruta surten de la mateixa declaració.
    const menuEntries: MenuEntryAmbPantalla[] = [
        { id: 'home', title: t('app.menu.home'), to: 'home', icon: icons.inici, pantalla: 'home' },
        { id: 'entitats', title: t('app.menu.entitats'), to: 'entitat', icon: icons.entitat, pantalla: 'entitat' },
        { id: 'avisos', title: t('app.menu.avisos'), to: 'avis', icon: icons.avis, pantalla: 'avis' },
        {
            id: 'documentsExclosos',
            title: t('app.menu.documentsExclosos'),
            to: 'documentExclos',
            icon: icons.documentExclos,
            pantalla: 'documentExclos',
        },
        { id: 'propietats', title: t('app.menu.propietats'), to: 'propietat', icon: icons.propietat, pantalla: 'propietat' },
        {
            id: 'integracions',
            title: t('app.menu.integracions'),
            to: 'integracio',
            icon: icons.integracio,
            pantalla: 'integracio',
        },
        {
            id: 'cacheDocuments',
            title: t('app.menu.cacheDocuments'),
            to: 'cacheDocument',
            icon: icons.cacheDocument,
            pantalla: 'cacheDocument',
        },
    ];

    const bgColor = mode === 'light' ? theme.palette.background.paper : undefined;
    const textColor = bgColor ? theme.palette.getContrastText(bgColor) : undefined;
    // CON_DRA_COL.png (el mateix que fa servir concsv-front) té els colors fixats i serveix per
    // als dos modes; si algun dia cal una variant per a fons foscos, fer-ne un ternari.
    const logoColor = concsvLogo;
    const { estilMenu } = useUserPreferences();

    return (
        <BaseApp
            code="CONCSV"
            logo={mode === 'light' ? goibLogoLight : goibLogoDark}
            logoStyle={{
                '& img': { height: LOGO_HEIGHT + 'px', display: 'block' },
                // El separador vertical ha de caure sobre la vora dreta del menú obert, i omplir
                // l'alçada de la capçalera. El botó de menú duu un marge esquerre de -12 i un dret de
                // 16, així que la caixa del logo arrenca a LOGO_BOX_LEFT. Fixant-ne l'amplada (en
                // comptes de deixar que la mida del logo mani) la vora cau sempre a MENU_WIDTH.
                width: MENU_WIDTH - LOGO_BOX_LEFT + 'px',
                boxSizing: 'border-box',
                alignSelf: 'stretch',
                borderRight: `1px solid ${theme.palette.divider}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'flex-end',
                // Substitueix el mr/pt/pr de la llibreria (l'sx s'hi fusiona al final).
                mr: 0,
                pt: 0,
                pl: 0,
                pr: LOGO_SEPARATOR_GAP + 'px',
            }}
            title={
                <img
                    style={{
                        marginLeft: LOGO_SEPARATOR_GAP + 'px',
                        height: LOGO_HEIGHT + 'px',
                        verticalAlign: 'middle',
                    }}
                    src={logoColor}
                    alt="ConCSV"
                />
            }
            version={version}
            menuEntries={filtrarEntradesMenu(menuEntries, currentRole)}
            menuAppearance={estilMenu}
            appbarBackgroundColor={bgColor}
            // El botó de menú duu ml -12, així que amb APPBAR_PADDING_LEFT queda centrat a la
            // mateixa columna que les icones del menú lateral. L'alçada mínima substitueix els 64px
            // que la llibreria dona a la barra.
            appbarStyle={{
                color: textColor,
                paddingLeft: APPBAR_PADDING_LEFT + 'px',
                minHeight: HEADER_HEIGHT + 'px',
            }}
            footerHeight={FOOTER_HEIGHT}
            // El peu va per flux, no fixat: l'arrel de l'aplicació ocupa l'alçada de la finestra,
            // així que queda sempre a baix i sense tapar el contingut.
            footer={
                <div style={{ height: FOOTER_HEIGHT + 'px', overflow: 'hidden' }}>
                    <DrassanaFooter title="CONCSV" backgroundColor="#5F5D5D" />
                </div>
            }
        >
            {/* Va per davant de l'<Outlet> perquè una pàgina que es posi el títol pel seu
                compte (useTitolPagina) sobreescrigui el que declara la ruta. */}
            <TitolPagina />
            <Outlet />
        </BaseApp>
    );
};

export const App = () => {
    const apiUrl = getEnvApiUrl();
    // Autenticació amb la sessió de servidor, sense token al navegador (veure ConcsvAuthProvider).
    return (
        <ConcsvAuthProvider apiUrl={apiUrl}>
            <ResourceApiProvider apiUrl={apiUrl}>
                {/* TemaProvider va per fora de tot, també de la pantalla de càrrega de
                    ConcsvProvider: arrenca amb l'últim tema conegut de l'usuari perquè no hi
                    hagi parpelleig mentre no arriba el perfil.

                    UserPreferencesProvider, en canvi, va per dins de ConcsvProvider: les
                    preferències (idioma, tema, estil de menú, mida de pàgina...) surten del perfil
                    que aquest carrega, i ConcsvProvider no pinta els fills fins a tenir-lo. */}
                <TemaProvider>
                    <SnackbarProvider maxSnack={99}>
                    <ConcsvProvider>
                        <UserPreferencesProvider>
                            <SessionStorageProvider>
                                <InnerApp />
                            </SessionStorageProvider>
                        </UserPreferencesProvider>
                    </ConcsvProvider>
                    </SnackbarProvider>
                </TemaProvider>
            </ResourceApiProvider>
        </ConcsvAuthProvider>
    );
};

export default App;
