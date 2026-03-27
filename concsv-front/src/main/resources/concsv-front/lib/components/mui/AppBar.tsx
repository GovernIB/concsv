import React from 'react';
import MuiAppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import Box from '@mui/material/Box';
import AuthButton from './AuthButton';
import { useOptionalAuthContext } from '../AuthContext';
import { toolbarBackgroundStyle } from '../../util/toolbar';

type AppBarProps = {
    title: string;
    titleLogo?: string;
    titleLogoStyle?: any;
    version?: string;
    logo?: string;
    logoStyle?: any;
    menuButton: React.ReactNode,
    additionalComponents?: React.ReactElement | React.ReactElement[];
    style?: any;
    backgroundColor?: string;
    backgroundImg?: string;
};

export const AppBar: React.FC<AppBarProps> = (props) => {
    const {
        title,
        titleLogo,
        titleLogoStyle,
        version,
        logo,
        logoStyle,
        menuButton,
        additionalComponents,
        style,
        backgroundColor,
        backgroundImg,
    } = props;
    const authContext = useOptionalAuthContext();
    const authButton = authContext != null ? <AuthButton /> : null;
    const backgroundStyle = backgroundColor ? toolbarBackgroundStyle(backgroundColor, backgroundImg) : {};
    return <MuiAppBar position="sticky" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
        <Toolbar style={{ ...style, ...backgroundStyle }}>
            {menuButton}
            {logo ? <Box sx={{ mr: 2, pt: 1, pr: 2, cursor: 'pointer', ...logoStyle }}>
                <img src={logo} alt="logo" />
            </Box> : null}
            {titleLogo && <Box sx={{ flexGrow: 1, display: 'flex', ...titleLogoStyle }}>
                <img src={titleLogo} alt={version ? title + ' v' + version : undefined} title={version ? title + ' v' + version : undefined} />
                </Box>}
            {additionalComponents}
            {authButton}
        </Toolbar>
    </MuiAppBar>;
}

export default AppBar;