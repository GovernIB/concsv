import { alpha, darken, lighten, type Theme } from '@mui/material/styles';

/** Colors semàntics de la paleta que admeten el tractament tonal. */
export type TonalColor = 'success' | 'error' | 'warning' | 'info' | 'primary' | 'secondary';

/**
 * Color tonal per a etiquetes i chips d'estat: fons translúcid del color i text enfosquit (tema clar) o aclarit
 * (temes foscos). Més suau que el color ple.
 *
 * Coeficients calculats amb les paletes de theme.ts (clar, fosc i Dracula), mesclant el fons translúcid amb el
 * paper i amb les files de la graella (parella, hover, seleccionada i seleccionada amb hover): el text supera
 * 4,5:1 (WCAG 2.1 AA) a tots els casos. El mínim és 4,6:1 (warning, fila seleccionada amb hover, tema clar).
 * Els de RIPEA (0,35 / 0,4) aquí no hi arriben: a Dracula fallen fins i tot sobre el paper.
 * Si es canvien els colors del tema, cal tornar a calcular-ho.
 *
 * No s'usen palette.dark/light: els temes foscos només defineixen main i els calculats no arriben al contrast.
 * Ús: sx={tonal('success')}, o tonal('success')(theme) si el component no accepta funcions al sx.
 */
export const tonal = (color: TonalColor) => (theme: Theme) => {
    const main = theme.palette[color].main;
    const fosc = theme.palette.mode === 'dark';
    return {
        bgcolor: alpha(main, fosc ? 0.2 : 0.14),
        color: fosc ? lighten(main, 0.7) : darken(main, 0.5),
        // Les icones dels chips i etiquetes agafen el color del text
        '& .MuiChip-icon, & .MuiIcon-root': { color: 'inherit' },
    };
};

/**
 * Variant neutra (estats inactius o sense color), amb el mateix aspecte que les tonals però sense color.
 * El text és text.primary: amb text.secondary no s'arriba a 4,5:1 a les files seleccionades.
 */
export const tonalNeutre = (theme: Theme) => ({
    bgcolor: alpha(theme.palette.text.primary, 0.08),
    color: theme.palette.text.primary,
    '& .MuiChip-icon, & .MuiIcon-root': { color: 'inherit' },
});

/** Si el valor és un color semàntic de la paleta amb tractament tonal. */
export const isTonalColor = (color: unknown): color is TonalColor =>
    ['success', 'error', 'warning', 'info', 'primary', 'secondary'].includes(color as string);

/**
 * Barra de progrés en to suau. La barra translúcida no arriba al contrast de 3:1 que es demana a un element
 * gràfic (WCAG 1.4.11) i el nivell no es pot transmetre només amb el color: s'ha d'acompanyar sempre del valor
 * en text (vegeu TonalLinearProgress).
 */
export const tonalProgress = (color: TonalColor) => (theme: Theme) => {
    const main = theme.palette[color].main;
    const fosc = theme.palette.mode === 'dark';
    return {
        bgcolor: alpha(main, fosc ? 0.12 : 0.1),
        '& .MuiLinearProgress-bar': { bgcolor: alpha(main, fosc ? 0.3 : 0.4) },
    };
};
