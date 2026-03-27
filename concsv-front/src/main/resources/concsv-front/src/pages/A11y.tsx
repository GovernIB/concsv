import React from 'react';
import { useTranslation } from 'react-i18next';
import Typography from '@mui/material/Typography';
import { BasePage } from '@programari-limit/base-react';

const A11y: React.FC = () => {
    const { t } = useTranslation();
    const rd = <a href="https://www.boe.es/diario_boe/txt.php?id=BOE-A-2018-12699">RD 1112/2018</a>;
    return <BasePage>
        <Typography variant="h4" gutterBottom sx={{ textAlign: 'center', mt: 2 }}>
            {t('page.a11y.title')}
        </Typography>
        <Typography gutterBottom>
            {t('page.a11y.p11')} <a href="https://www.boe.es/diario_boe/txt.php?id=BOE-A-2018-12699">{t('page.a11y.reialDecret')}</a> {t('page.a11y.p12')}
        </Typography>
        <Typography gutterBottom>
        {t('page.a11y.p21')} <a href="https://csv.caib.es/concsv">https://csv.caib.es/concsv</a> {t('page.a11y.p22')}
        </Typography>

        <Typography variant="h6" gutterBottom>
            {t('page.a11y.situacio.title')}
        </Typography>
        <Typography sx={{ ml: 2 }} gutterBottom>
            {t('page.a11y.situacio.p11')} {rd}{t('page.a11y.situacio.p12')}
        </Typography>
        <Typography sx={{ ml: 2, mt: 2 }} gutterBottom>
            <b>{t('page.a11y.situacio.contingut.title')}</b>
        </Typography>
        <Typography component="div" sx={{ ml: 4 }} gutterBottom>
            {t('page.a11y.situacio.contingut.p1')}
            <ul style={{ marginTop: '8px' }}>
                <li>{t('page.a11y.situacio.contingut.m1')} {rd}.</li>
                <ul>
                    <li>{t('page.a11y.situacio.contingut.m11')}</li>
                    <li>{t('page.a11y.situacio.contingut.m12')}</li>
                </ul>
                <li>{t('page.a11y.situacio.contingut.m2')}</li>
                <ul>
                    <li>{t('page.a11y.situacio.contingut.m21')}</li>
                </ul>
            </ul>
        </Typography>

        <Typography variant="h6" gutterBottom>
            {t('page.a11y.preparacio.title')}
        </Typography>
        <Typography sx={{ ml: 2 }} gutterBottom>
            {t('page.a11y.preparacio.p1')}
        </Typography>
        <Typography sx={{ ml: 2 }} gutterBottom>
            {t('page.a11y.preparacio.p2')}
        </Typography>
        <Typography sx={{ ml: 2 }} gutterBottom>
            {t('page.a11y.preparacio.p3')}
        </Typography>

        <Typography variant="h6" gutterBottom>
            {t('page.a11y.observacions.title')}
        </Typography>
        <Typography component="div" sx={{ ml: 1 }} gutterBottom>
            {t('page.a11y.observacions.p11')} {rd} {t('page.a11y.observacions.p12')}
            <ul style={{ marginTop: '8px' }}>
                <li>{t('page.a11y.observacions.o1')}</li>
                <li>{t('page.a11y.observacions.o2')}</li>
                <li>{t('page.a11y.observacions.o3')}</li>
            </ul>
        </Typography>
        <Typography component="div" sx={{ ml: 1 }} gutterBottom>
            {t('page.a11y.observacions.p21')} <a href={t('page.a11y.observacions.form.url')}>{t('page.a11y.observacions.form.title')}</a> {t('page.a11y.observacions.p22')}
            <ul style={{ marginTop: '8px' }}>
                <li>{t('page.a11y.observacions.o4')} {rd}.</li>
                <li>{t('page.a11y.observacions.o5')}</li>
                <ul>
                    <li>{t('page.a11y.observacions.o511')} {rd} {t('page.a11y.observacions.o512')}</li>
                    <li>{t('page.a11y.observacions.o52')}</li>
                </ul>
            </ul>
        </Typography>
        <Typography sx={{ ml: 2 }} gutterBottom>
            {t('page.a11y.observacions.p3')} <a href={t('page.a11y.observacions.procediment.url')}>{t('page.a11y.observacions.procediment.name')}</a>.
        </Typography>
        <Typography sx={{ ml: 2, mt: 2 }} gutterBottom>
            <b>{t('page.a11y.observacions.procediment.title')}</b>
        </Typography>
        <Typography sx={{ ml: 4 }} gutterBottom>
            {t('page.a11y.observacions.procediment.p11')} {rd} {t('page.a11y.observacions.procediment.p12')}
        </Typography>
        <Typography sx={{ ml: 4 }} gutterBottom>
            {t('page.a11y.observacions.procediment.p2')}
        </Typography>
        <Typography sx={{ ml: 4, mb: 4 }} gutterBottom>
            {t('page.a11y.observacions.procediment.p3')} <a href={t('page.a11y.observacions.procediment.reclamacio.url')}>{t('page.a11y.observacions.procediment.reclamacio.title')}</a>.
        </Typography>
    </BasePage>;
}

export default A11y;