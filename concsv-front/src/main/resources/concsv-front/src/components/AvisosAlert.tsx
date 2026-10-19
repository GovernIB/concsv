import React from 'react';
import { useTranslation } from 'react-i18next';
import { envVar, useAppContext } from '@programari-limit/base-react';
import Alert from '@mui/material/Alert';
import AlertTitle from '@mui/material/AlertTitle';
import Collapse from '@mui/material/Collapse';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import { envVars } from '../main';

interface AvisResource {
  id: number;
  assumpte: string;
  missatge: string;
  dataInici: string;
  dataFinal?: string | null;
  actiu: boolean;
  avisNivell: 'INFO' | 'WARNING' | 'ERROR';
  entitat?: any;
}

interface AvisosResponse {
  _embedded: {
    avisos: AvisResource[];
  };
  _links: any;
}

const AvisosAlert: React.FC = () => {
  const { t } = useTranslation();
  const { currentLanguage } = useAppContext();
  
  const [avisos, setAvisos] = React.useState<AvisResource[]>([]);
  const [loading, setLoading] = React.useState<boolean>(true);
  const [expandedId, setExpandedId] = React.useState<number | null>(null);

  React.useEffect(() => {
    const fetchAvisos = async () => {
      try {
        setLoading(true);
        const baseUrl = envVar('VITE_API_URL', envVars) || '/api';
        const url = `${baseUrl}/avisos/actius?lang=${currentLanguage}`;
        const response = await fetch(url);
        if (response.ok) {
          const data: AvisosResponse = await response.json();
          const listaAvisos = data._embedded?.avisos || [];
          const avisosActius = listaAvisos.filter((a) => a.actiu);
          setAvisos(avisosActius);
        }
      } catch (error) {
        console.error('Error cargando avisos:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchAvisos();
  }, [currentLanguage]);

  const handleToggleExpand = (id: number) => {
    setExpandedId(expandedId === id ? null : id);
  };

  const getSeverity = (nivell: string): 'info' | 'warning' | 'error' | 'success' => {
    switch (nivell) {
      case 'ERROR': return 'error';
      case 'WARNING': return 'warning';
      case 'INFO': 
      default: return 'info';
    }
  };

  if (loading || avisos.length === 0) {
    return null;
  }

  return (
    <Box sx={{ width: '100%', mb: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
      {avisos.map((aviso) => (
        <Alert 
          key={aviso.id} 
          severity={getSeverity(aviso.avisNivell)}
          sx={{ 
            alignItems: 'flex-start',
            '& .MuiAlert-message': { width: '100%' }
          }}
          action={
            <IconButton
              aria-label={expandedId === aviso.id ? t('common.collapse') : t('common.expand')}
              aria-expanded={expandedId === aviso.id}
              size="small"
              onClick={() => handleToggleExpand(aviso.id)}
            >
              <Icon sx={{ fontSize: 20 }}>
                {expandedId === aviso.id ? 'expand_less' : 'expand_more'}
              </Icon>
            </IconButton>
          }
        >
          <AlertTitle sx={{ fontWeight: 'bold', mb: 0.5 }}>
            {aviso.assumpte}
          </AlertTitle>
          
          <Collapse in={expandedId === aviso.id} timeout="auto" unmountOnExit>
            <Typography variant="body2" sx={{ mt: 1, whiteSpace: 'pre-wrap' }}>
              {aviso.missatge}
            </Typography>
            {aviso.dataFinal && (
              <Typography variant="caption" sx={{ display: 'block', mt: 1, opacity: 0.8 }}>
                {t('page.avisos.validFinsA')}: {new Date(aviso.dataFinal).toLocaleDateString(currentLanguage)}
              </Typography>
            )}
          </Collapse>
        </Alert>
      ))}
    </Box>
  );
};

export default AvisosAlert;