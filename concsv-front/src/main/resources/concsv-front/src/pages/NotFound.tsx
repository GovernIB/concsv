import { useTranslation } from 'react-i18next';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import Typography from '@mui/material/Typography';
import { BasePage } from '@programari-limit/base-react';

const NotFound: React.FC = () => {
    const { t } = useTranslation();
    return <BasePage>
        <Box sx={{display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 1,mt: 2}}>
            <Box sx={{ textAlign: 'center' }}>
                <Icon sx={{ fontSize: '81.92px', color: 'rgba(200, 0, 0, 0.5)' }}>highlight_off</Icon>
            </Box>
            <Box sx={{ textAlign: 'center', alignSelf: 'center'}}>
                <Typography variant="h4" sx={{ textAlign: 'center', alignSelf: 'center'}}>
                    404 {t('page.notFound.title')}
                </Typography>
            </Box>
        </Box>
        <br/>
        <Typography variant="h6" gutterBottom sx={{ textAlign: 'center', mb: 3 }}>
            {t('page.notFound.description')}
        </Typography>
    </BasePage>;
}

export default NotFound;