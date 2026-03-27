import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ThemeProvider } from '@emotion/react';
import { CssBaseline } from '@mui/material';
import theme from './theme';
import App from './App.tsx';
import { envVar, ResourceApiProvider } from '@programari-limit/base-react';

export const envVars = {
    VITE_API_URL: import.meta.env.VITE_API_URL,
    VITE_API_BASE_URL: import.meta.env.VITE_API_BASE_URL,
    VITE_API_SUFFIX: import.meta.env.VITE_API_SUFFIX,
    VITE_PREVIEW_ENABLED: import.meta.env.VITE_PREVIEW_ENABLED,
    VITE_RECAPTCHA_ENABLED: import.meta.env.VITE_RECAPTCHA_ENABLED,
    VITE_RECAPTCHA_SITE_KEY: import.meta.env.VITE_RECAPTCHA_SITE_KEY,
}

const getEnvApiUrl = () => {
    const envApiUrl = envVar('VITE_API_URL', envVars);
    if (envApiUrl) {
        return envApiUrl;
    } else {
        const envApiBaseUrl = envVar('VITE_API_BASE_URL', envVars);
        const envApiSuffix = envVar('VITE_API_SUFFIX', envVars) ?? '/api';
        if (envApiBaseUrl) {
            return envApiBaseUrl + envApiSuffix;
        } else {
            return window.location.protocol + '//' + window.location.host + ':' + window.location.port + envApiSuffix;
        }
    }
}

ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
        <ResourceApiProvider apiUrl={getEnvApiUrl()}>
        <ThemeProvider theme={theme}>
            <CssBaseline />
            <BrowserRouter basename={import.meta.env.BASE_URL}>
                <App />
            </BrowserRouter>
            </ThemeProvider>
        </ResourceApiProvider>
    </React.StrictMode>,
)
