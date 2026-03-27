import React from 'react';
import {
    useNavigate,
    useLocation,
    Link as RouterLink,
    LinkProps as RouterLinkProps,
} from 'react-router-dom';
import i18n from '../i18n/i18n';
import { useTranslation } from 'react-i18next';
import Toolbar from '@mui/material/Toolbar';
import Button from '@mui/material/Button';
import Icon from '@mui/material/Icon';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import logo from '../assets/GOVERN_IB_V_COL.svg';
import logoConcsv from '../assets/CON_DRA_COL.png';
import logoFooter from '../assets/DRA_B.png';
import { MuiBaseApp, MenuEntry } from '@programari-limit/base-react';
import LanguageSelectorButton from './LanguageSelectorButton';

export type HeaderBackgroundModuleItem = {
    color?: string;
    image?: string;
}

export type BaseCaibAppProps = React.PropsWithChildren & {
    code: string;
    title: string;
    version: string;
    adminMenuTitle?: string;
    onModuleChange?: (moduleId: string) => void;
    availableModules?: string[];
    availableLanguages?: any;
    defaultLanguage?: string;
    menuEntries?: MenuEntry[];
    headerBackgroundColor?: string;
    headerBackgroundImage?: string;
};

export type CaibMenuToolbarProps = {
    menuEntries?: MenuEntry[];
    isSmallScreen?: boolean;
}

export type CaibFooterProps = {
    title: string;
    version?: string;
}

const Link = React.forwardRef<HTMLAnchorElement, RouterLinkProps>((itemProps, ref) => {
    return <RouterLink ref={ref} {...itemProps} role={undefined} />;
});
const LinkBehavior = React.forwardRef<any, Omit<RouterLinkProps, 'to'>>(
    (props, ref) => <RouterLink ref={ref} to="/" {...props} role={undefined} />,
);

const commonLogoImgStyles = {
    backgroundColor: 'white',
    height: '70px',
    position: 'absolute',
    top: '0',
    // borderLeft: '1px solid #ccc',
    borderRight: '1px solid #ccc',
}
const logoStyle = {
    width: '160px',
    ['& img']: {
        ...commonLogoImgStyles,
        padding: '0 16px',
        width: '160px',
    }
};
const logoStyleSm = {
    width: '110px',
    ['& img']: {
        ...commonLogoImgStyles,
        padding: '16px 8px',
    },
};

const commonTitleLogoImgStyles = {
    backgroundColor: 'white',
    height: '70px',
    position: 'absolute',
    top: '0',
}
const titleLogoStyle = {
    width: '110px',
    ['& img']: {
        ...commonTitleLogoImgStyles,
        padding: '8px 0',
    },
}

const titleLogoStyleSm = {
    width: '110px',
    ['& img']: {
        ...commonTitleLogoImgStyles,
        padding: '16px 0',
    }
}

const headerStyle = {
    height: '70px',
    boxShadow: 'none'
};

export const manifestEntry = (name: string) => {
    const manifest = (window as any).__MANIFEST__;
    if (manifest && manifest.hasOwnProperty(name)) {
        return manifest[name];
    } else {
        return undefined;
    }
};

const mapUserMenuEntries = (entries: any[]): MenuEntry[] => {
    return entries?.map((e: any) => ({
        id: e.id,
        title: e.title,
        icon: e.icon,
        to: '/' + e.resource,
        children: mapUserMenuEntries(e.children)
    }));
}

const useLocationPath = () => {
    const location = useLocation();
    return location.pathname;
}

const CaibMenuToolbarButton: React.FC<any> = (props) => {
    const { entry } = props;
    return <Button
        component={LinkBehavior}
        {...{ to: entry.to }}
        sx={{ mx: 1, color: '#666' }}>
        {entry.icon && <Icon sx={{ fontSize: 16, mb: '4px', mr: '6px' }}>{entry.icon}</Icon>}
        {entry.title}
    </Button>;
}

const CaibMenuToolbar: React.FC<CaibMenuToolbarProps> = (props) => {
    const { menuEntries, isSmallScreen } = props;
    const menuHeight = '40px';
    return <>
        <Box sx={{ height: menuHeight }} />
        <Toolbar
            disableGutters
            sx={{
                position: 'fixed',
                top: '78px',
                width: '100%',
                minHeight: menuHeight + ' !important',
                height: menuHeight,
                mt: -1,
                mx: -2,
                pl: isSmallScreen ? 1 : 2,
                backgroundColor: '#ededed',
                borderBottom: '1px solid #ccc',
                zIndex: 10,
            }}>
            {menuEntries?.map(e => <CaibMenuToolbarButton key={e.id} entry={e} />)}
        </Toolbar>
    </>;
}

const CaibFooter: React.FC<CaibFooterProps> = (props) => {
    const { title, version } = props;
    const theme = useTheme();
    const { t } = useTranslation();
    const footerHeight = '40px';
    /*
    const showManifest = useMediaQuery(theme.breakpoints.up('lg'));
    const manifestInfo = showManifest ? <span style={{ color: 'transparent', fontSize: '10px' }}>
        &nbsp;{manifestEntry('Build-Timestamp')} {manifestEntry('Implementation-SCM-Revision')}
    </span> : null;
    */
    const isSmallScreen = useMediaQuery(theme.breakpoints.down('sm'));

    return <>
        <Box sx={{ height: footerHeight, mt: 2 }} />
        <Toolbar
            disableGutters
            sx={{
                width: '100%',
                minHeight: footerHeight + ' !important',
                mt: -6,
                backgroundColor: '#4d4d4d',
                borderTop: '1px solid #ccc'
            }}>
            <Box sx={{
                display: 'flex',
                flexDirection: isSmallScreen ? 'column' : 'row',
                justifyContent: 'space-between',
                color: '#999999',
                width: '100%',
                mx: isSmallScreen ? 0 : 4
            }}>
                <Box sx={{
                    position: 'relative',
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                    borderRadius: '1.6rem',
                    p: 3,
                }}>


                    <Typography sx={{ fontWeight: 'bold', color: 'white' }}>
                        {title} v{version}
                    </Typography>
                    <Typography sx={{ fontWeight: 'bold' }}>
                        {t('footer.revision')}&nbsp;
                        <Typography component={'span'} sx={{ fontStyle: 'italic' }}>
                            {manifestEntry('Implementation-SCM-Revision')}
                        </Typography>
                    </Typography>
                </Box>
                <Box sx={{
                    display: 'flex',
                    ['& img']: {
                        height: '80px',
                        padding: '8px 0',
                    },
                }}>
                    <img src={logoFooter} alt={version ? title + ' v' + version : undefined} title={version ? title + ' v' + version : undefined} />
                </Box>
            </Box>
        </Toolbar>
    </>;
}

export const BaseCaibApp: React.FC<BaseCaibAppProps> = (props) => {
    const {
        code,
        title,
        version,
        defaultLanguage,
        menuEntries,
        headerBackgroundColor,
        headerBackgroundImage,
        children
    } = props;
    const navigate = useNavigate();
    const location = useLocation();
    const theme = useTheme();
    const i18nHandleLanguageChange = (language?: string) => {
        i18n.changeLanguage(language);
    }
    const i18nAddResourceBundleCallback = (language: string, namespace: string, bundle: any) => {
        i18n.addResourceBundle(language, namespace, bundle);
    }
    const anyHistoryEntryExist = () => location.key !== 'default';
    const goBack = (fallback?: string) => {
        if (anyHistoryEntryExist()) {
            navigate(-1);
        } else if (fallback != null) {
            navigate(fallback);
        } else {
            console.warn('[BACK] Couldn\'t go back, neither fallback specified nor previous entry exists in navigation history');
        }
    }
    const isSmallScreen = useMediaQuery(theme.breakpoints.down('sm'));
    return <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
        <Box sx={{ flex: 1 }}>
            <MuiBaseApp
                code={code}
                title={title}
                titleLogo={logoConcsv}
                titleLogoStyle={isSmallScreen ? titleLogoStyleSm : titleLogoStyle}
                version={version}
                logo={logo}
                logoStyle={isSmallScreen ? logoStyleSm : logoStyle}
                persistentSession
                //persistentLanguage
                i18nUseTranslation={useTranslation}
                i18nCurrentLanguage={defaultLanguage ?? i18n.language}
                i18nHandleLanguageChange={i18nHandleLanguageChange}
                i18nAddResourceBundleCallback={i18nAddResourceBundleCallback}
                routerGoBack={goBack}
                routerUseLocationPath={useLocationPath}
                routerAnyHistoryEntryExist={anyHistoryEntryExist}
                linkComponent={Link}
                //menuEntries={menuEntries}
                additionalHeaderComponents={[
                    <LanguageSelectorButton key="sel_lang"/>
                ]}
                appbarStyle={headerStyle}
                appbarBackgroundColor={headerBackgroundColor}
                appbarBackgroundImg={headerBackgroundImage}>
                <CaibMenuToolbar menuEntries={menuEntries} isSmallScreen={isSmallScreen} />
                {children}
            </MuiBaseApp>
        </Box>
        <CaibFooter title={title} version={version} />
    </Box>;
}

export default BaseCaibApp;