import React from 'react';
import { useTranslation } from 'react-i18next';
import { Box, Grid, Icon, IconButton, ListItemButton } from '@mui/material';
import ListItem from '@mui/material/ListItem';
import { FormField, MuiForm, useFormContext } from 'reactlib';
import type { PropietatItem } from './PropietatsTypes';
import { TIPUS_CREDENCIALS, getFieldFromItem } from './PropietatsUtils';
import { TextHighlight } from './TextHighlight';

type PropsListItemProps = {
    highlight?: string;
};

/**
 * Fila d'una propietat: descripció, camp de valor segons el tipus i les accions disponibles.
 *
 * - Les propietats del fitxer de propietats del servidor (`jbossProperty`) es mostren en només
 *   lectura, amb el valor efectiu.
 * - Si la base de dades no té valor, el camp mostra `alternativeValue` (el valor del fitxer del
 *   servidor, que és el que s'aplica) com a placeholder o, a les caselles, com a valor.
 */
const PropsListItem: React.FC<PropsListItemProps> = ({ highlight }) => {
    const { t } = useTranslation();
    const { data, apiRef } = useFormContext();
    const item = data as PropietatItem;

    const disabled = item.jbossProperty;
    const password = item.type.id === TIPUS_CREDENCIALS;
    const decimalScale = item.type.id === 'INT' ? 0 : undefined;
    const field = getFieldFromItem(item, t);

    // El formulari ja mostra l'error del servidor (p. ex. un valor que no és d'aquest tipus)
    const save = () => {
        apiRef.current?.save().catch(() => undefined);
    };

    return (
        <Grid container spacing={2} sx={{ width: '100%' }}>
            <Grid size={4.5}>
                <TextHighlight text={item.description} match={highlight} ignoreCase />
            </Grid>
            <Grid size={6}>
                <FormField
                    field={field}
                    name={'value'}
                    value={field.value}
                    inline
                    decimalScale={decimalScale}
                    disabled={disabled}
                    componentProps={{
                        type: password ? 'password' : field.type,
                        autoComplete: password ? 'new-password' : undefined,
                        placeholder: item.alternativeValue || item.key,
                        helperText: <TextHighlight text={item.key} match={highlight} ignoreCase />,
                    }}
                />
            </Grid>
            <Grid size={1.5}>
                <Box sx={{ display: 'flex', justifyContent: 'end' }}>
                    {!disabled && (
                        <IconButton title={t('common.save')} size="small" onClick={save} color={'success'}>
                            <Icon fontSize="small">save</Icon>
                        </IconButton>
                    )}
                </Box>
            </Grid>
        </Grid>
    );
};

type PropietatsFormProps = PropsListItemProps & {
    id: string;
    onSaved?: (item: PropietatItem) => void;
};

/** Formulari (independent) d'una propietat, que carrega i desa el recurs per la seva clau. */
export const PropietatsForm: React.FC<PropietatsFormProps> = ({ id, highlight, onSaved }) => (
    <MuiForm
        key={id}
        id={id}
        resourceName="configResource"
        hiddenToolbar
        formBlockerDisabled
        onUpdateSuccess={(saved: PropietatItem) => onSaved?.(saved)}
        commonFieldComponentProps={{ size: 'small' }}
    >
        <ListItem disablePadding>
            <ListItemButton disableRipple>
                <PropsListItem highlight={highlight} />
            </ListItemButton>
        </ListItem>
    </MuiForm>
);

export default PropietatsForm;
