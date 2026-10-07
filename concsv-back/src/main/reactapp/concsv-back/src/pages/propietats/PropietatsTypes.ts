/** Referència (id i descripció) a un altre recurs, tal com la torna l'API. */
export interface PropietatReferencia {
    id: string;
    description?: string;
}

/**
 * Propietat configurable (`configResource`). `value` és el del fitxer del servidor per a les
 * propietats d'aquest fitxer i el de la BD per a les editables (buit si ningú no n'ha desat cap).
 * En aquest darrer cas `alternativeValue` duu el valor del fitxer, que és el que s'aplica.
 */
export interface PropietatItem {
    id: string;
    key: string;
    value?: string | null;
    alternativeValue?: string | null;
    description: string;
    jbossProperty: boolean;
    position: number;
    group: PropietatReferencia;
    /** `description` duu, quan el tipus és una enumeració, la llista de valors vàlids separats per coma. */
    type: PropietatReferencia;
}

/** Grup de propietats (`configGroupResource`). */
export interface PropietatGrup {
    id: string;
    key: string;
    description: string;
    position: number;
    parent?: PropietatReferencia | null;
}
