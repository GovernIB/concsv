import React from 'react';
import { useTranslation } from 'react-i18next';
import Divider from '@mui/material/Divider';
import Grid from '@mui/material/Grid';
import { useFormContext } from 'reactlib';
import GridFormField from '../../components/GridFormField';

/**
 * Camps del formulari (modal) de creació i modificació d'entitats.
 * - `codi` no es pot canviar un cop creada l'entitat: n'és l'identificador funcional.
 * - `activa` no hi és: les entitats es creen actives i l'estat es canvia amb les accions
 *   Activa/Desactiva del menú de la fila (veure EntitatAccions).
 * Els labels i les validacions (obligatori, mida màxima) els aporta el backend (EntitatResource).
 */
export const EntitatFormContent: React.FC = () => {
    const { t } = useTranslation();
    const { data } = useFormContext();
    return (
        <Grid container spacing={2}>
            <GridFormField size={4} name="codi" disabled={data?.id != null} />
            <GridFormField size={8} name="nom" />
            <GridFormField size={12} name="descripcio" />
            <GridFormField size={6} name="cif" />
            <GridFormField size={6} name="codiDir3" />
            <Grid size={12}>
                <Divider>{t('page.entitats.form.seccioAparenca')}</Divider>
            </Grid>
            {/* type="color": selector de color del navegador, que desa el valor en hexadecimal
                (#rrggbb), directament aplicable en CSS. El backend també ho valida. */}
            <GridFormField size={3} name="colorFons" type="color" />
            <GridFormField size={3} name="colorLletra" type="color" />
            <GridFormField size={3} name="colorFonsDark" type="color" />
            <GridFormField size={3} name="colorLletraDark" type="color" />
            <GridFormField size={6} name="logoImgFile" />
            <GridFormField size={6} name="logoImgFileDark" />
        </Grid>
    );
};

export default EntitatFormContent;
