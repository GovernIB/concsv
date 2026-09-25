import { createBrowserRouter } from 'react-router-dom';
import App from './App';
import ProtectedRoute from './components/ProtectedRoute';
import RutaInicial from './components/RutaInicial';
import Home from './pages/Home';
import NotFound from './pages/NotFound';
import EnConstruccio from './pages/EnConstruccio';
import EntitatGrid from './pages/entitat/EntitatGrid';
import type { Pantalla } from './util/pantalles';

/**
 * Ruta d'un manteniment encara no implementat: ja té el control d'accés definitiu i mostra la
 * pantalla provisional. Per implementar-lo n'hi ha prou amb substituir-ne l'element.
 */
const rutaEnConstruccio = (pantalla: Pantalla, path: string, titol: string) => ({
    element: <ProtectedRoute pantalla={pantalla} />,
    children: [
        {
            path,
            element: <EnConstruccio titol={titol} />,
            handle: { titol },
        },
    ],
});

export const router = createBrowserRouter(
    [
        {
            path: '/',
            element: <App />,
            children: [
                {
                    index: true,
                    element: <RutaInicial />,
                },
                // El "handle" de cada ruta duu la clau de traducció del títol de la pestanya del
                // navegador (veure TitolPagina). Cada pantalla penja d'un ProtectedRoute que en
                // comprova el rol (veure PANTALLA_ROLS a util/pantalles.ts): una ruta nova ha
                // d'anar dins d'un d'aquests grups, o quedaria oberta a tots els rols.
                {
                    element: <ProtectedRoute pantalla="home" />,
                    children: [
                        {
                            path: 'home',
                            element: <Home />,
                            handle: { titol: 'app.menu.home' },
                        },
                    ],
                },
                {
                    element: <ProtectedRoute pantalla="entitat" />,
                    children: [
                        {
                            path: 'entitat',
                            element: <EntitatGrid />,
                            handle: { titol: 'page.entitats.grid.title' },
                        },
                    ],
                },
                rutaEnConstruccio('avis', 'avis', 'app.menu.avisos'),
                rutaEnConstruccio('documentExclos', 'documentExclos', 'app.menu.documentsExclosos'),
                rutaEnConstruccio('propietat', 'propietat', 'app.menu.propietats'),
                rutaEnConstruccio('integracio', 'integracio', 'app.menu.integracions'),
                rutaEnConstruccio('cacheDocument', 'cacheDocument', 'app.menu.cacheDocuments'),
                // També és necessari perquè l'iframe de renovació silenciosa de sessió
                // (oidc-client-ts) munti App en navegar a "oidcSilentRenew".
                {
                    path: '*',
                    element: <NotFound />,
                    handle: { titol: 'page.notFound.message' },
                },
            ],
        },
    ],
    {
        basename: import.meta.env.BASE_URL,
    }
);
