import React from 'react';
import { useTranslation } from 'react-i18next';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Collapse from '@mui/material/Collapse';
import Divider from '@mui/material/Divider';
import Icon from '@mui/material/Icon';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import { MuiDialog, useBaseAppContext, useCloseDialogButtons, useResourceApiService } from 'reactlib';
import { IntegracioStacktrace } from './IntegracioStacktrace';

const INTEGRACIO_RESOURCE = 'monitorIntegracioResource';
const ACTION_INTEGRACIONS_DIAGNOSTIC = 'INTEGRACIONS_DIAGNOSTIC';
const ACTION_DIAGNOSTIC = 'DIAGNOSTIC';

/** Resposta de l'acció INTEGRACIONS_DIAGNOSTIC */
interface IntegracionsDiagnosticResult {
    codis: string[];
}

/** Resposta de l'acció DIAGNOSTIC */
interface DiagnosticResult {
    correcte: boolean;
    prova?: string;
    errMsg?: string;
    excepcioStacktrace?: string;
}

/** Estat de la prova d'una integració: sense resultat mentre s'executa. */
interface DiagnosticEstat {
    executant: boolean;
    resultat?: DiagnosticResult;
}

// eslint-disable-next-line react-refresh/only-export-components
const DiagnosticFila: React.FC<{
    codi: string;
    estat?: DiagnosticEstat;
    onExecuta: (codi: string) => void;
}> = ({ codi, estat, onExecuta }) => {
    const { t } = useTranslation();
    const { temporalMessageShow } = useBaseAppContext();
    const [tracaOberta, setTracaOberta] = React.useState(false);
    const resultat = estat?.resultat;
    const executant = estat == null || estat.executant;
    const stacktrace = !executant ? resultat?.excepcioStacktrace : undefined;

    React.useEffect(() => {
        if (executant) {
            setTracaOberta(false);
        }
    }, [executant]);

    const copiaTraca = () => {
        if (stacktrace) {
            navigator.clipboard?.writeText(stacktrace);
            temporalMessageShow(null, t('common.copiat'), 'success');
        }
    };

    return (
        <Stack spacing={1}>
            <Stack direction="row" alignItems="center" spacing={2}>
                <Typography variant="subtitle2" sx={{ width: 180, flexShrink: 0 }}>
                    {t(`page.integracio.pipella.${codi}`)}
                </Typography>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                    {executant ? (
                        <Stack direction="row" alignItems="center" spacing={1} sx={{ py: 1 }}>
                            <CircularProgress size={20} />
                            <Typography variant="body2" color="text.secondary">
                                {t('page.integracio.diagnostic.executant')}
                            </Typography>
                        </Stack>
                    ) : (
                        <Alert
                            severity={resultat?.correcte ? 'success' : 'error'}
                            action={
                                stacktrace && (
                                    <>
                                        <Tooltip title={t('page.integracio.diagnostic.copiaTraca')}>
                                            <IconButton color="inherit" size="small" onClick={copiaTraca}>
                                                <Icon fontSize="small">content_copy</Icon>
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip
                                            title={t(
                                                tracaOberta
                                                    ? 'page.integracio.diagnostic.amagaTraca'
                                                    : 'page.integracio.diagnostic.mostraTraca'
                                            )}
                                        >
                                            <IconButton
                                                color="inherit"
                                                size="small"
                                                onClick={() => setTracaOberta((oberta) => !oberta)}
                                            >
                                                <Icon fontSize="small">
                                                    {tracaOberta ? 'expand_less' : 'expand_more'}
                                                </Icon>
                                            </IconButton>
                                        </Tooltip>
                                    </>
                                )
                            }
                            sx={{ '& .MuiAlert-message': { flex: 1, minWidth: 0, overflow: 'hidden' } }}
                        >
                            <Typography
                                variant="body2"
                                noWrap
                                title={resultat?.correcte ? resultat?.prova : resultat?.errMsg}
                            >
                                {resultat?.correcte ? resultat?.prova : resultat?.errMsg}
                            </Typography>
                            {stacktrace && (
                                <Collapse in={tracaOberta} unmountOnExit>
                                    <Box sx={{ pt: 1 }}>
                                        <IntegracioStacktrace stacktrace={stacktrace} senseCapcalera />
                                    </Box>
                                </Collapse>
                            )}
                        </Alert>
                    )}
                </Box>
                <Tooltip title={t('page.integracio.diagnostic.executa')}>
                    <span>
                        <IconButton onClick={() => onExecuta(codi)} disabled={executant}>
                            <Icon>refresh</Icon>
                        </IconButton>
                    </span>
                </Tooltip>
            </Stack>
        </Stack>
    );
};

/**
 * Diagnòstic de les integracions: en obrir-se executa la prova de totes les integracions a la vegada. 
 * Cadascuna es pot tornar a executar per separat o es poden tornar a executar totes.
 * Si una prova falla es mostra el motiu.
 */
export const useIntegracioDiagnostic = () => {
    const { t } = useTranslation();
    const closeButtons = useCloseDialogButtons();
    const { isReady: apiIsReady, artifactAction } = useResourceApiService(INTEGRACIO_RESOURCE);
    const [open, setOpen] = React.useState(false);
    const [codis, setCodis] = React.useState<string[]>([]);
    const [estats, setEstats] = React.useState<Record<string, DiagnosticEstat>>({});
    const [errorCarrega, setErrorCarrega] = React.useState<string>();
    const execucionsRef = React.useRef<Record<string, number>>({});

    const setEstat = (codi: string, estat: DiagnosticEstat) => setEstats((prev) => ({ ...prev, [codi]: estat }));

    const executa = (codi: string) => {
        const execucio = (execucionsRef.current[codi] ?? 0) + 1;
        execucionsRef.current[codi] = execucio;
        const esVigent = () => execucionsRef.current[codi] === execucio;
        setEstat(codi, { executant: true });
        artifactAction(undefined, {
            code: ACTION_DIAGNOSTIC,
            data: { codiIntegracio: codi },
        })
            .then((resultat: DiagnosticResult) => {
                if (esVigent()) {
                    setEstat(codi, { executant: false, resultat });
                }
            })
            .catch((error: any) => {
                if (esVigent()) {
                    setEstat(codi, {
                        executant: false,
                        resultat: { correcte: false, errMsg: error?.message },
                    });
                }
            });
    };

    const executaTotes = (codisIntegracio: string[]) => codisIntegracio.forEach(executa);

    const invalidaExecucions = () => {
        Object.keys(execucionsRef.current).forEach((codi) => execucionsRef.current[codi]++);
    };

    const handleOpen = () => {
        if (!apiIsReady) {
            return;
        }
        setEstats({});
        setErrorCarrega(undefined);
        setOpen(true);
        artifactAction(undefined, { code: ACTION_INTEGRACIONS_DIAGNOSTIC })
            .then((result: IntegracionsDiagnosticResult) => {
                const codisIntegracio = result?.codis ?? [];
                setCodis(codisIntegracio);
                executaTotes(codisIntegracio);
            })
            .catch((error: any) => {
                setCodis([]);
                setErrorCarrega(error?.message);
            });
    };

    const handleClose = (reason?: string) => {
        if (reason !== 'backdropClick') {
            invalidaExecucions();
            setOpen(false);
        }
    };

    const algunaExecutant = codis.some((codi) => estats[codi] == null || estats[codi].executant);

    const dialog = (
        <MuiDialog
            open={open}
            closeCallback={handleClose}
            title={t('page.integracio.diagnostic.titol')}
            componentProps={{ fullWidth: true, maxWidth: 'lg' }}
            buttons={closeButtons}
            buttonCallback={() => handleClose()}
        >
            {errorCarrega ? (
                <Alert severity="error">{errorCarrega}</Alert>
            ) : (
                <Stack spacing={1} divider={<Divider />} sx={{ pt: 1 }}>
                    <Stack direction="row" justifyContent="flex-end">
                        <Button
                            variant="contained"
                            color="success"
                            startIcon={<Icon sx={{ mr: 1 }}>refresh</Icon>}
                            onClick={() => executaTotes(codis)}
                            disabled={codis.length === 0 || algunaExecutant}
                            sx={{ mb: 1 }}
                        >
                            {t('page.integracio.diagnostic.executaTotes')}
                        </Button>
                    </Stack>
                    {codis.map((codi) => (
                        <DiagnosticFila key={codi} codi={codi} estat={estats[codi]} onExecuta={executa} />
                    ))}
                </Stack>
            )}
        </MuiDialog>
    );

    return { apiIsReady, show: handleOpen, component: dialog };
};

export default useIntegracioDiagnostic;
