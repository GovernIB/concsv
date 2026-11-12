import React from 'react';
import { useTranslation } from 'react-i18next';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import { GridPage } from 'reactlib';
import ScreenFrame from '../components/ScreenFrame';
import { alpha } from '@mui/material/styles';
import { accentColor } from '../theme';
import { icons } from '../util/icons';

const Home: React.FC = () => {
    const { t } = useTranslation();
    return (
        <GridPage>
            <ScreenFrame
                fitContent
                icon={icons.inici}
                title={t('page.home.toolbar.title')}
                actions={
                    // El xip cau sobre la part del degradat que arriba al color principal: el fons tonal
                    // (translúcid) s'hi perd. Pren l'aspecte dels botons de la barra (titleBarControlsSx):
                    // fons del paper, text d'accent i vora d'accent translúcida.
                    <Chip
                        size="small"
                        variant="outlined"
                        label={`v${import.meta.env.VITE_APP_VERSION}`}
                        sx={(theme) => ({
                            bgcolor: theme.palette.background.paper,
                            color: theme.palette.mode === 'dark' ? theme.palette.text.primary : accentColor(theme),
                            borderColor: alpha(accentColor(theme), 0.4),
                            fontWeight: 600,
                        })}
                    />
                }
            >
                <Typography variant="body1" color="text.secondary">
                    {t('page.home.toolbar.subtitle')}
                </Typography>
            </ScreenFrame>
        </GridPage>
    );
};

export default Home;
