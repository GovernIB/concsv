import React from 'react';

/**
 * Nom de l'aplicació que encapçala sempre el títol de la pestanya del navegador.
 */
export const APP_TITOL = 'CONCSV';

/**
 * Fixa el títol de la pestanya: "CONCSV - <títol>", o només "CONCSV" si no se'n passa cap.
 */
export const setTitolPagina = (titol?: string) => {
    document.title = titol ? `${APP_TITOL} - ${titol}` : APP_TITOL;
};

/**
 * Versió en forma de hook de setTitolPagina. Cada pàgina hi passa el títol ja traduït que mostra
 * a la capçalera, així que en canviar d'idioma el text canvia i el títol es torna a fixar.
 *
 * El router d'aquesta aplicació no és un data router (no hi ha useMatches), per això el títol no
 * es declara a la ruta com a concsv-back sinó a cada pàgina.
 */
export const useTitolPagina = (titol?: string) => {
    React.useEffect(() => {
        setTitolPagina(titol);
    }, [titol]);
};

export default useTitolPagina;
