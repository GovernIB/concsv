import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import TextField from '@mui/material/TextField';
import InputAdornment from '@mui/material/InputAdornment';
import Button from '@mui/material/Button';
import Icon from '@mui/material/Icon';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Recaptcha from 'react-google-recaptcha';
import { BasePage, useAppContext, envVar } from '@programari-limit/base-react';
import { envVars } from '../main';


const isRecaptchaShow = () => {
    const recaptchaEnvVar = envVar('VITE_RECAPTCHA_ENABLED', envVars);
    return recaptchaEnvVar != null && typeof recaptchaEnvVar === 'string' ? recaptchaEnvVar?.toLowerCase() === 'true' : !!recaptchaEnvVar;
}

const RecaptchaElement: React.FC<any> = (props) => {
    const { onChange } = props;
    const recaptchaSiteKey = envVar('VITE_RECAPTCHA_SITE_KEY', envVars);
    const recaptchaRef = React.createRef<any>();
    const { currentLanguage } = useAppContext();
    return <Recaptcha
        ref={recaptchaRef}
        hl={currentLanguage}
        sitekey={recaptchaSiteKey}
        onChange={onChange} />;
}

const Home: React.FC = () => {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [csv, setCsv] = React.useState<string>('');
    const [validationError, setValidationError] = React.useState<string>();
    const [recaptchaValid, setRecaptchaValid] = React.useState<boolean>(!isRecaptchaShow());
    const handleValidateClick = () => {
        setValidationError(undefined);
        if (!csv) {
            setValidationError(t('page.home.invalid.empty'));
        } else if (csv.length < 32) {
            setValidationError(t('page.home.invalid.length'));
        } else {
            navigate('/view/' + csv);
        }
    }
  
    const handleKeyDown = (event: any) => {
      if (event.key === "Enter" || event.code === 'NumpadEnter') {
        event.preventDefault(); // evitar submit accidental
        if (csv && recaptchaValid) {
          handleValidateClick();
        }
      }
    };

    const handleRecaptchaChange = (value: any) => {
        setRecaptchaValid(value != null);
    }
    
    const submitButton = (
      <Button
        variant="contained"
        size="large"
        disabled={!recaptchaValid}
        onClick={handleValidateClick}
        fullWidth
        sx={{ py: 2, height: "76px" }}
      >
        <Icon sx={{ mr: 1 }}>check_circle_outline</Icon>
        {t("page.home.validate")}
      </Button>
    );
    
    return <BasePage>
        <Typography variant="h4" gutterBottom sx={{ textAlign: 'center', mt: 2 }}>
            {t('page.home.title')}
        </Typography>
        <Typography gutterBottom sx={{ textAlign: 'justify' }}>
            {t('page.home.p1')}
        </Typography>
        <Typography gutterBottom sx={{ textAlign: 'justify' }}>
            {t('page.home.p2')}
        </Typography>
        <TextField
            value={csv}
            onChange={(event) => setCsv(event.target.value)}
            error={validationError != null}
            helperText={validationError}
            placeholder={t('page.home.placeholder')}
            variant="outlined"
            required
            fullWidth
            onKeyDown={handleKeyDown}
            InputProps={{
                endAdornment: <InputAdornment position="end"><Icon>search</Icon></InputAdornment>,
            }}
            sx={{ mt: 2 }} />
        <Box sx={{ display: 'flex', mt: 2 }}>
            {isRecaptchaShow() ? <>
                <Box sx={{ width: '300px' }}>
                    <RecaptchaElement onChange={handleRecaptchaChange} />
                </Box>
                <Box sx={{ ml: 2, width: 'calc(100% - 300px)' }}>
                    {submitButton}
                </Box>
            </> : submitButton}
        </Box>
    </BasePage>;
}

export default Home;