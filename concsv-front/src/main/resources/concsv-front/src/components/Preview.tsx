import React from 'react';
import { useTranslation } from 'react-i18next';
import Alert from '@mui/material/Alert';
import { Box } from '@mui/material';


export type PreviewProps = {
    mainUrl: string;
    fallbackUrl?: string;
    show?: boolean;
    onLoad?: () => void;
    onError?: () => void;
    isSmallScreen?: boolean;
    printableFailed?: boolean;
};

const Preview: React.FC<PreviewProps> = (props) => {
    const {
        mainUrl,
        fallbackUrl,
        show,
        onLoad,
        onError,
        isSmallScreen,
        printableFailed
    } = props;
    const { t } = useTranslation();
    const objectRef = React.useRef<HTMLObjectElement>(null);
    const [previewError, setPreviewError] = React.useState<boolean>(printableFailed ?? false);
    const [previewError2, setPreviewError2] = React.useState<boolean>(false);
    const handlePreviewLoad = () => {
        onLoad?.();
    }
    const handlePreviewError = () => {
        if (!previewError) {
            // Primer error - Versió imprimible
            if (fallbackUrl != null) {
                objectRef.current?.setAttribute('data', fallbackUrl);
            } else {
                onError?.();
            }
            setPreviewError(true);
        } else {
            // Segon error - Versió original
            onError?.();
            setPreviewError2(true);
        }
    }
    // React.useEffect(() => {
    //     objectRef.current?.addEventListener('load', handlePreviewLoad);
    //     objectRef.current?.addEventListener('error', handlePreviewError);
    //     return () => {
    //         objectRef.current?.removeEventListener('load', handlePreviewLoad);
    //         objectRef.current?.removeEventListener('error', handlePreviewError);
    //     }
    // }, [objectRef.current]);
    
    React.useEffect(() => {
        if (!show) {
            setPreviewError(false);
            setPreviewError2(false);
        }
    }, [show]);
    return show &&
        <Box sx={{ display: 'flex', flexDirection: 'column', width: isSmallScreen ? '100%' : '60%' }}>
            {previewError && <Alert severity="error" sx={{ mb: 1 }}>
                {t('page.view.found.preview.error' + (!previewError2 ? '1' : '2'))}
            </Alert>}
            {!previewError && <object
                ref={objectRef}
                onLoad={handlePreviewLoad}
                onError={handlePreviewError}
                data={mainUrl}
                style={{ width: '100%', height: '100vh' }}
                type="application/pdf" />}
            {previewError && fallbackUrl && <object
                ref={objectRef}
                onLoad={handlePreviewLoad}
                onError={handlePreviewError}
                data={fallbackUrl}
                style={{ width: '100%', height: '90vh' }}
                type="application/pdf" />}
        </Box>;
}

export default Preview;