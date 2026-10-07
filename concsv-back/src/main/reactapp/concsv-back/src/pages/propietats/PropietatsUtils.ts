import type { PropietatItem } from './PropietatsTypes';

export type TranslateFn = (key: string, options?: Record<string, unknown>) => string;

/** Codi del tipus de les propietats que contenen credencials (CSV_CONFIG_TYPE). */
export const TIPUS_CREDENCIALS = 'CREDENTIALS';

/**
 * Valors d'una propietat BOOL que es mostren com a marcats (sense distingir majúscules). Mateix
 * criteri que `ConfigValues.isTrue` al backend: algunes claus usen S/N en lloc de true/false.
 */
export const VALORS_BOOLEANS_CERTS = ['true', 's', '1', 'yes', 'y', 'on'];

/** Indica si el valor d'una propietat BOOL es considera cert. */
export const esBooleaCert = (value: unknown): boolean =>
    typeof value === 'string' && VALORS_BOOLEANS_CERTS.includes(value.trim().toLowerCase());

/** Text de l'error d'una crida a l'API (o del que s'hagi llançat). */
export const missatgeError = (error: unknown): string => {
    const detall = error as { description?: string; message?: string } | null;
    return detall?.description ?? detall?.message ?? String(error);
};

/**
 * Etiqueta traduïda d'un dels valors vàlids d'un tipus de propietat (CSV_CONFIG_TYPE). El valor que
 * es desa a la base de dades no canvia: si no hi ha traducció definida es mostra tal qual.
 */
export const configTypeValueLabel = (t: TranslateFn, typeCode: string, value: string): string =>
    t(`enum.configType.${typeCode}.${value}`, { defaultValue: value, nsSeparator: false });

const fieldPropType = (typeCode: string, typeValue?: string): string => {
    if (typeValue != null) {
        return 'search';
    }
    switch (typeCode) {
        case 'INT':
        case 'FLOAT':
            return 'number';
        case 'BOOL':
            return 'checkbox';
        default:
            return 'text';
    }
};

/**
 * Definició del camp de formulari d'una propietat a partir del seu tipus: la descripció del tipus
 * conté, quan és una enumeració, la llista de valors vàlids separats per coma.
 */
export const getFieldFromItem = (item: PropietatItem, t: TranslateFn) => {
    const type = fieldPropType(item.type.id, item.type.description);
    const options = item.type.description
        ? Object.fromEntries(
              item.type.description
                  .split(',')
                  .map((v: string) => [v, configTypeValueLabel(t, item.type.id, v)])
          )
        : undefined;
    // Una casella sense valor a la BD es mostra amb el del fitxer del servidor, que és el que s'aplica
    const valorCasella = item.value ?? item.alternativeValue;
    const value =
        item.type.id === 'BOOL' && typeof valorCasella === 'string' ? esBooleaCert(valorCasella) : item.value;
    return {
        label: item.description,
        name: item.key,
        type,
        value,
        options,
    };
};
