// Retorna un array amb les components RGB d'un color en format #rrggbb
export const hexColorToRgbArray = (c: string): number[] => { // c in format '#rrggbb'
    const hexStr = c.substring(1);
    const hex = hexStr.length == 3 ? hexStr.replace(/(.)/g, '$1$1') : hexStr;
    const dec = parseInt(hex, 16);
    return [(dec >> 16) & 255, (dec >> 8) & 255, dec & 255];
}

// Retorna un objecte amb les components RGB d'un color en format #rrggbb
export const hexColorToRgbObject = (c: string): any => { // c in format '#rrggbb'
    const rgbArray = hexColorToRgbArray(c);
    return { r: rgbArray[0], g: rgbArray[1], b: rgbArray[2] };
}

// Retorna la luminància d'un color en format #rrggbb
// Segons https://www.w3.org/TR/WCAG20/
export const getRelativeLuminance = (c: string): number => {
    const rgb = hexColorToRgbArray(c);
    return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
}

// Retorna el contrast entre dos colors en format #rrggbb
// Segons https://www.w3.org/TR/WCAG20/
export const getContrast = (c1: string, c2: string) => {
    const l1 = getRelativeLuminance(c1);
    const l2 = getRelativeLuminance(c2);
    return (l1 > l2) ? (l1 + 0.05) / (l2 + 0.05) : (l2 + 0.05) / (l1 + 0.05);
}

// Retorna el color de text que resaltaria més sobre un color de fons.
// Tots els colors s'han de passar en format #rrggbb
export const getTextColorOnBackground = (
    text1: string,
    text2: string,
    background: string) => {
    return getContrast(text1, background) > getContrast(text2, background) ? text1 : text2;
}