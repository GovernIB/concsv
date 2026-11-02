import React from 'react';
import { useTranslation } from 'react-i18next';
import Chip from '@mui/material/Chip';
import Typography from '@mui/material/Typography';
import { GridPage } from 'reactlib';
import ScreenFrame from '../components/ScreenFrame';
import { icons } from '../util/icons';
import { tonal } from '../util/tonal';

const Home: React.FC = () => {
    const { t } = useTranslation();
    return (
        <GridPage>
            <ScreenFrame
                fitContent
                icon={icons.inici}
                title={t('page.home.toolbar.title')}
                actions={
                    // Sense color="primary": la sobreescriptura de MuiChip de theme.ts hi posaria el fons ple
                    <Chip size="small" label={`v${import.meta.env.VITE_APP_VERSION}`} sx={tonal('primary')} />
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
