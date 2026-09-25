import React from 'react';

export const ROLE_PREFIX = 'CSV_';
export const ROLE_SUPER = ROLE_PREFIX + 'SUPER';
// Rol base que el backend concedeix a qualsevol usuari autenticat (no és cap rol de Keycloak).
export const ROLE_USER = 'tothom';

export type ConcsvContextType = {
    isReady: boolean;
    currentUser: any;
    setCurrentUser: (currentUser: any | undefined) => void;
    rolesAvailable?: string[];
    entitatsAvailable?: any[];
    currentRole?: string;
    setCurrentRole: (currentRole: string | undefined) => void;
    currentEntitatId?: number;
    setCurrentEntitatId: (currentEntitatId: number | undefined) => void;
    currentEntitatLoading?: boolean;
    currentEntitat?: any;
};

export const ConcsvContext = React.createContext<ConcsvContextType | undefined>(undefined);

export const useConcsvContext = () => {
    const context = React.useContext(ConcsvContext);
    if (context === undefined) {
        throw new Error('useConcsvContext must be used within a ConcsvProvider');
    }
    return context;
};
