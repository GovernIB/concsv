import React from 'react';

export const ROLE_PREFIX = 'CSV_';
// Únic rol amb què es pot entrar a la interfície (veure ConcsvProvider).
export const ROLE_SUPER = ROLE_PREFIX + 'SUPER';

export type ConcsvContextType = {
    isReady: boolean;
    currentUser: any;
    setCurrentUser: (currentUser: any | undefined) => void;
    entitatsAvailable?: any[];
    currentRole?: string;
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
