import type { MenuEntry } from 'reactlib';
import { ROLE_SUPER, ROLE_USER } from '../components/ConcsvContext';

/**
 * Rols amb els quals es pot operar a la interfície REACT: els mateixos que ofereix el selector de
 * rol (veure ALLOWED_ROLES a ConcsvProvider).
 */
export const ROLS_APLICACIO = [ROLE_SUPER, ROLE_USER];

/** Identificador de cada pantalla amb control d'accés. Una pantalla nova s'ha d'afegir aquí. */
export type Pantalla =
    | 'home'
    | 'entitat'
    | 'avis'
    | 'documentExclos'
    | 'propietat'
    | 'integracio'
    | 'cacheDocument';

/**
 * Rols autoritzats per pantalla. És l'única font de veritat del control d'accés de la interfície:
 * la fan servir tant el filtre del menú lateral (App.tsx) com les guardes de ruta (router.tsx), de
 * manera que amagar una entrada de menú i barrar-ne la ruta no poden divergir. El tipus Pantalla
 * obliga a declarar-hi tota pantalla nova.
 *
 * Compte: això és usabilitat, no seguretat. Qui protegeix de veritat és el backend amb els
 * @ResourceAccessConstraint de cada classe *Resource. Tota pantalla nova ha de declarar les seves
 * restriccions al recurs encara que aquí ja s'amagui.
 */
export const PANTALLA_ROLS: Record<Pantalla, string[]> = {
    home: ROLS_APLICACIO,
    entitat: [ROLE_SUPER],
    avis: [ROLE_SUPER],
    documentExclos: [ROLE_SUPER],
    propietat: [ROLE_SUPER],
    integracio: [ROLE_SUPER],
    cacheDocument: [ROLE_SUPER],
};

export const isPantallaPermesa = (pantalla: Pantalla, rol?: string): boolean =>
    rol != null && PANTALLA_ROLS[pantalla].includes(rol);

/**
 * Pantalla d'inici per rol. Avui cap rol no en té una de pròpia (tots poden entrar a /home), però
 * és el punt on afegir-la quan n'hi hagi. La ruta que s'hi indiqui ha d'estar autoritzada per al
 * rol a PANTALLA_ROLS.
 */
const RUTA_INICIAL_PER_ROL: Partial<Record<string, string>> = {};

export const RUTA_INICIAL_DEFECTE = '/home';

export const rutaInicialPerRol = (rol?: string): string =>
    (rol != null ? RUTA_INICIAL_PER_ROL[rol] : undefined) ?? RUTA_INICIAL_DEFECTE;

/**
 * Entrada de menú amb la pantalla a la qual dona accés. Les entrades sense pantalla (separadors,
 * agrupadors o enllaços externs) es mostren a tots els rols.
 */
export type MenuEntryAmbPantalla = MenuEntry & {
    pantalla?: Pantalla;
    children?: MenuEntryAmbPantalla[];
};

/**
 * Deixa al menú només les entrades que el rol pot obrir. Els submenús es filtren en profunditat i
 * els que es queden sense fills visibles i no són cap enllaç desapareixen.
 */
export const filtrarEntradesMenu = (
    entrades: MenuEntryAmbPantalla[],
    rol?: string
): MenuEntry[] =>
    entrades.reduce<MenuEntry[]>((visibles, entrada) => {
        const { pantalla, children, ...entradaMenu } = entrada;
        if (pantalla != null && !isPantallaPermesa(pantalla, rol)) {
            return visibles;
        }
        if (children != null) {
            const fills = filtrarEntradesMenu(children, rol);
            if (fills.length === 0 && entradaMenu.to == null) {
                return visibles;
            }
            visibles.push({ ...entradaMenu, children: fills });
        } else {
            visibles.push(entradaMenu);
        }
        return visibles;
    }, []);
