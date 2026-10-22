/**
 * Catàleg d'icones (Material Icons) per **concepte**. És l'única font d'icones de l'aplicació:
 * l'entrada de menú, el títol de la pantalla i els botons que representen el mateix concepte han de
 * fer servir sempre la mateixa icona, i el mateix concepte ha de tenir la mateixa icona a totes les
 * aplicacions de Drassana. Per això el fitxer és autònom: es pot copiar tal qual a una altra app, hi
 * canvien només les entrades de pantalla.
 *
 * Si una pantalla o acció nova necessita una icona, s'hi afegeix aquí per concepte; no es posa el nom
 * de la icona en línia al component.
 */
export const icons = {
    // --- Pantalles (entrades de menú i títol de la pantalla) ---------------------------------
    inici: 'home',
    entitat: 'account_balance',
    avis: 'campaign',
    documentExclos: 'block',
    propietat: 'display_settings',
    integracio: 'sync_alt',
    cacheDocument: 'cached',
    configuracio: 'settings',
    monitoritzacio: 'monitor',

    // --- Accions -----------------------------------------------------------------------------
    crea: 'add',
    modifica: 'edit',
    esborra: 'delete',
    esborraTot: 'delete_sweep',
    desa: 'save',
    desfes: 'undo',
    cancela: 'close',
    accepta: 'check',
    activa: 'check',
    desactiva: 'close',
    executa: 'bolt',
    refresca: 'refresh',
    sincronitza: 'cached',
    copia: 'content_copy',
    descarrega: 'file_download',
    carrega: 'file_upload',
    detall: 'info',
    diagnostic: 'monitor_heart',
    plega: 'expand_less',
    desplega: 'expand_more',

    // --- Filtre i cerca ----------------------------------------------------------------------
    cerca: 'search',
    filtre: 'filter_alt',
    neteja: 'filter_alt_off',
    cercaAvancadaObre: 'zoom_in',
    cercaAvancadaTanca: 'zoom_out',

    // --- Selecció (accions massives) ---------------------------------------------------------
    seleccionaTot: 'check_box',
    deseleccionaTot: 'check_box_outline_blank',
} as const;

export type IconName = (typeof icons)[keyof typeof icons];
