import React from 'react';

/**
 * Indica als camps d'un filtre que aquest s'aplica sol (FilterAutoApply): en lloc de gestionar-ne el
 * retard cada camp, el gestiona el filtre sencer. Va en un fitxer a part perquè el fitxer del component
 * només exporti components (react-refresh).
 */
export const FilterAutoApplyContext = React.createContext(false);

/** `true` si el component és dins un filtre amb aplicació automàtica. */
export const useFilterAutoApply = () => React.useContext(FilterAutoApplyContext);

/**
 * Tipus de camp que l'usuari escriu tecla a tecla: el filtre n'espera el final abans d'aplicar-se. La
 * resta (desplegables, booleans, referències...) són una sola acció i s'apliquen de seguida.
 */
export const TYPING_FIELD_TYPES = ['text', 'string', 'textarea', 'url', 'number', 'integer', 'decimal', 'email'];

/** Un camp sense tipus conegut es tracta com a text. */
export const isTypingFieldType = (type?: string) => TYPING_FIELD_TYPES.includes(type ?? 'text');
