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
 * - Els botons de desfer i desar només estan habilitats si el valor s'ha modificat.
 * - Si la base de dades no té valor, el camp mostra `alternativeValue` (el valor del fitxer del
 *   servidor, que és el que s'aplica) com a placeholder o, a les caselles, com a valor.
 */
const PropsListItem: React.FC<PropsListItemProps> = ({ highlight }) => {
    const { t } = useTranslation();
    const { data, apiRef, modified } = useFormContext();
    const item = data as PropietatItem;

    const disabled = item.jbossProperty;
    const password = item.type.id === TIPUS_CREDENCIALS;
    const decimalScale = item.type.id === 'INT' ? 0 : undefined;
    const field = getFieldFromItem(item, t);

    const save = () => {
        apiRef.current?.save().catch(() => undefined);
    };

    // Torna al valor carregat o desat per últim cop, sense demanar confirmació
    const undo = () => {
        apiRef.current?.revert(true);
    };

    return (
        <Grid container spacing={2} sx={{ width: '100%' }}>
            <Grid size={4.5}>
                <TextHighlight text={item.description} match={highlight} ignoreCase />
            </Grid>
            <Grid size={6.5}>
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
                        placeholder: item.alternativeValue || undefined,
                        helperText: <TextHighlight text={item.key} match={highlight} ignoreCase />,
                    }}
                />
            </Grid>
            <Grid size={1}>
                <Box sx={{ display: 'flex', justifyContent: 'end' }}>
                    {!disabled && (
                        <>
                            <IconButton title={t('common.undo')} size="small" onClick={undo} disabled={!modified}>
                                <Icon fontSize="small">undo</Icon>
                            </IconButton>
                            <IconButton
                                title={t('common.save')}
                                size="small"
                                onClick={save}
                                color={'success'}
                                disabled={!modified}
                            >
                                <Icon fontSize="small">save</Icon>
                            </IconButton>
                        </>
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
