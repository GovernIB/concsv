import React from 'react';
import { Navigate } from 'react-router-dom';
import { rutaInicialPerRol } from '../util/pantalles';
import { useConcsvContext } from './ConcsvContext';

/**
 * Pantalla d'entrada de la SPA: redirigeix a la pantalla d'inici del rol actual (equivalent al
 * HomeRedirect de RIPEA, ripea-back/src/main/jsapp/ripea-back/src/AppRoutes.tsx:71). Hi arriba tant
 * qui obre l'arrel de l'aplicació com qui hi torna després de canviar de rol.
 */
export const RutaInicial: React.FC = () => {
    const { currentRole } = useConcsvContext();
    return <Navigate to={rutaInicialPerRol(currentRole)} replace />;
};

export default RutaInicial;
