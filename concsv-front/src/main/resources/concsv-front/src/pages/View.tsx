import React from 'react';
import { useTranslation } from 'react-i18next';
import { useParams, useSearchParams } from 'react-router-dom';
import { saveAs } from 'file-saver';
import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import { useTheme } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import {
    BasePage,
    useAppContext,
    useResourceApiService,
    envVar
} from '@programari-limit/base-react';
import Preview from '../components/Preview';
import { envVars } from '../main';
import { Alert } from '@mui/material';

const SignerRow: React.FC<any> = (props) => {
    const {
        title,
        value,
        variant,
        comment,
        style
    } = props;
    return value && <>
        <Typography variant="subtitle1" gutterBottom sx={{ textAlign: 'center' }}>
            {title ? title + ': ' : ''} <span style={style}>{value}</span>
        </Typography>
        {comment && <Typography variant={variant ?? 'subtitle1'} gutterBottom sx={{ textAlign: 'center' }}>
            {comment}
        </Typography>}
    </>;
}

const SignerRepresentat: React.FC<any> = (props) => {
    const { signer } = props;
    const { t } = useTranslation();
    return signer?.representat && <Typography variant="subtitle1" gutterBottom sx={{ textAlign: 'center' }}>
        {t('page.view.found.firmants.representat.part1')} {signer.representatRaoSocial}
        {' ' + t('page.view.found.firmants.representat.part2')} {signer.representatNifCif}
    </Typography>;
}

const ContentDivider: React.FC = () => {
  return (
    <Box sx={{ display: "flex", justifyContent: "center", my: 2 }}>
      <Divider sx={{ width: "200px" }} />
    </Box>
  );
};

const Firmants: React.FC<any> = (props) => {
  const { data } = props;
  const { t } = useTranslation();
  const firmantsAmbError =
    data.signers?.filter((s: any) =>
      s.signerCN?.toLowerCase().includes("error")
    ).length > 0;
  const anyDataSignatura =
    data.signers?.filter((s: any) => s.dataSignatura != null).length > 0;
  const signersCount = data.signers ? data.signers.length : 0;

  return (
    <Box>
      <Typography
        variant="h5"
        gutterBottom
        sx={{ textAlign: "center", fontWeight: "200" }}
      >
        {t("page.view.found.firmants.title")}
      </Typography>
      {firmantsAmbError ? (
        <Typography gutterBottom sx={{ textAlign: "center" }}>
          <Icon color="error" sx={{ position: "relative", top: "4px" }}>
            warning
          </Icon>
          {t("page.view.found.firmants.error")}
        </Typography>
      ) : (
        <>
          {data.eniSignType === 'TF01' && <SignerRow value={t("page.view.found.firmants.csv")} variant="body1" />}
          {signersCount > 0 && <ContentDivider />}
          {data.signers.map((s: any, i: number) => (
            <Box key={i}>
              <SignerRow value={s.signerCN} style={{ fontWeight: "bold" }} />
              <SignerRepresentat signer={s} />
              <SignerRow value={s.sistemaComponent} variant="body2" />
              <SignerRow value={s.puesto} variant="body2" />
              <SignerRow value={s.signerOU} variant="body2" />
              <SignerRow value={s.signerO} variant="body2" />
              <SignerRow
                title={t("page.view.found.firmants.dataSignatura.title") + "*"}
                value={s.dataSignatura}
              />
              {s.reason && <SignerRow title={t("page.view.found.firmants.rao")} value={s.reason} variant="body2"/>}
              <ContentDivider />
            </Box>
          ))}
          {data.darrerSegell && (
            <Typography
              variant="body1"
              gutterBottom
              sx={{ textAlign: "center" }}
            >
              {t("page.view.found.firmants.segell")} {data.darrerSegell}
            </Typography>
          )}
          {anyDataSignatura && (
            <Typography
              variant="body2"
              gutterBottom
              sx={{ textAlign: "center", color: "#666" }}
            >
              * {t("page.view.found.firmants.dataSignatura.comment")}
            </Typography>
          )}
          <Typography
            variant="body2"
            gutterBottom
            sx={{ textAlign: "center", color: "#666" }}
          >
            {anyDataSignatura && data.signers?.length > 0 && <>
              *&nbsp;
              {t("page.view.found.firmants.dataSignatura.order")}
            </>}
          </Typography>
        </>
      )}
    </Box>
  );
};

const EniMetadataRow: React.FC<any> = (props) => {
  const { title, value } = props;
  return (
    <>
      <Typography variant="body2" gutterBottom sx={{ textAlign: "center" }}>
        <span style={{ fontWeight: "bold", color: "#666" }}>{title}</span>
      </Typography>
      <Typography
        variant="body2"
        gutterBottom
        sx={{ textAlign: "center", mb: 1 }}
      >
        {value}
      </Typography>
    </>
  );
};

const MetadadesEni: React.FC<any> = (props) => {
  const { data } = props;
  const { t } = useTranslation();

  return (
    <Box>
      <Typography
        variant="h5"
        gutterBottom
        sx={{ textAlign: "center", fontWeight: "200" }}
      >
        {t("page.view.found.metadades.title")}
      </Typography>
      <ContentDivider />
      <EniMetadataRow
        title={t("page.view.found.metadades.nom")}
        value={data.documentName}
      />
      <EniMetadataRow
        title={t("page.view.found.metadades.csv")}
        value={data.hash}
      />
      <EniMetadataRow
        title={t("page.view.found.metadades.identificador")}
        value={data.eniDocumentId}
      />
      <EniMetadataRow
        title={t("page.view.found.metadades.tipusDoc")}
        value={t("page.view.found.metadades.eni.tipus." + data.eniDocumentType)}
      />
      <EniMetadataRow
        title={t("page.view.found.metadades.estat")}
        value={t(
          "page.view.found.metadades.eni.estatElaboracio." +
            data.eniElaborationStatus
        )}
      />
      <EniMetadataRow
        title={t("page.view.found.metadades.versio")}
        value={data.eniNtiVersion}
      />
      <EniMetadataRow
        title={t("page.view.found.metadades.organ")}
        value={data.eniOrgan}
      />
      {data.eniOrigin != null && (
        <EniMetadataRow
          title={t("page.view.found.metadades.origen")}
          value={t("page.view.found.metadades.eni.origen." + data.eniOrigin)}
        />
      )}
      <EniMetadataRow
        title={t("page.view.found.metadades.tipusSig")}
        value={t(
          "page.view.found.metadades.eni.signaturaTipus." + data.eniSignType
        )}
      />
      <EniMetadataRow
        title={t("page.view.found.metadades.dataCaptura")}
        value={data.datacaptura}
      />
      <ContentDivider />
      <Typography
        variant="body2"
        gutterBottom
        sx={{ textAlign: "center", color: "#666", mb: 2 }}
      >
        {t("page.view.found.metadades.evidencies")}
      </Typography>
    </Box>
  );
};

const ViewLoading: React.FC = () => {
  const { t } = useTranslation();
  return (
    <BasePage>
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "calc(100vh - 182px)",
        }}
      >
        <Box sx={{ mt: -10 }}>
          <Box sx={{ textAlign: "center", mb: 2 }}>
            <CircularProgress size={"80px"} />
          </Box>
          <Typography variant="h6" gutterBottom sx={{ textAlign: "center" }}>
            {t("page.view.loading.title")}
          </Typography>
        </Box>
      </Box>
    </BasePage>
  );
};

const ViewError: React.FC<any> = (props) => {
  const { /*error,*/ handleGoBack } = props;
  const { t } = useTranslation();

  return (
    <BasePage>
      <Typography variant="h4" gutterBottom sx={{ textAlign: "center" }}>
        {t("page.view.error.title")}
      </Typography>
      <Box sx={{ mt: 2, textAlign: "center" }}>
        <Icon sx={{ fontSize: 128, color: "rgba(200, 0, 0, 0.5)" }}>
          error_outline
        </Icon>
      </Box>
      <Typography variant="h6" gutterBottom sx={{ textAlign: "center" }}>
        {t("page.view.error.description")}
      </Typography>
      <Typography gutterBottom sx={{ textAlign: "center", mt: 4 }}>
        {t("page.view.error.description")}
      </Typography>
      <Box
        sx={{ display: "flex", justifyContent: "center", width: "100%", mt: 2 }}
      >
        <Button variant="contained" onClick={handleGoBack} sx={{ py: 1 }}>
          <Icon sx={{ mr: 1 }}>arrow_back</Icon> {t("page.view.error.back")}
        </Button>
      </Box>
    </BasePage>
  );
};

const ViewFound: React.FC<any> = (props) => {
    const {
      data,
      loadingOriginal,
      loadingPrintable,
      handleOriginalClick,
      handlePrintableClick,
      previewBaseUrl,
      printableFailed,
      setPrintableFailed
    } = props;
    const { t } = useTranslation();
    const theme = useTheme();
    const isSmallScreen = useMediaQuery(theme.breakpoints.down("md"));
    const noImprimible = !data.printable || !!data.hasPassword || !!data.malformatted;
    const previewEnabled = envVar('VITE_PREVIEW_ENABLED', envVars)?.toLowerCase() === 'true';
    const previewByDefault = previewEnabled && !noImprimible;
    const [previewShow, setPreviewShow] = React.useState<boolean>(previewByDefault);
    const [loadingPreview, setLoadingPreview] = React.useState<boolean>(previewByDefault);
    
    const handlePreviewClick = () => {
      if (!previewShow) {
        setLoadingPreview(true);
        setPreviewShow(true);
      } else {
        setPreviewShow(false);
        setLoadingPreview(false);
      }
      // setPreviewShow((v) => !v);
    };
    
    const handlePreviewLoad = () => {
      setLoadingPreview(false);
    };

    const handlePreviewError = () => {
      setLoadingPreview(false);
      setPrintableFailed(true);
    };

    const { currentLanguage } = useAppContext();

    return <BasePage>
        <Box sx={{ display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 1, mt: 2 }}>
            <Box sx={{ textAlign: 'center' }}>
                <Icon sx={{ fontSize: '81.92px', color: 'rgba(165, 220, 134, 1)' }}>check_circle_outline</Icon>
            </Box>
            <Typography variant="h4" sx={{ textAlign: 'center', alignSelf: 'center' }}>
                {t('page.view.found.title')}
            </Typography>
        </Box>
        <Typography variant="h6" gutterBottom sx={{ textAlign: 'center' }}>
            {data.documentName}
        </Typography>
        {data.hasPassword === true && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1 }}>
                <Alert severity="warning" sx={{ display: 'inline-flex', textAlign: 'center' }} >
                    {t('page.view.found.download.hasPassword')}
                </Alert>
            </Box>
        )}
        {data.malformatted === true && (
            <Box sx={{ display: 'flex', justifyContent: 'center', mt: 1 }}>
                <Alert severity="warning" sx={{ display: 'inline-flex', textAlign: 'center' }} >
                    {t('page.view.found.download.malformatted')}
                </Alert>
            </Box>
        )}
        <Box sx={{ display: 'flex', flexDirection: isSmallScreen ? 'column' : 'row', justifyContent: 'center', width: '100%', mt: 4 }}>
        	{ 
				(!data.amagarBotoOriginal || noImprimible || printableFailed) &&
	        	(<Button
	                variant="contained"
	                disabled={data.csvExclos}
	                onClick={handleOriginalClick}
	                sx={{ py: 1, mb: 2, mr: !isSmallScreen ? 2 : 0 }}>
	                {loadingOriginal ? <CircularProgress size={24} color="inherit" sx={{ mr: 1 }} /> : <Icon sx={{ mr: 1 }}>cloud_download</Icon>}
	                {t('page.view.found.download.original')}
	            </Button>)
	         }
            <span
                title={noImprimible ? t('page.view.found.download.notavailable') : undefined}
                style={!isSmallScreen ? { marginRight: '16px' } : undefined}>
                <Button
                    variant="contained"
                    disabled={noImprimible || (printableFailed && !data.csvExclos)}
                    onClick={handlePrintableClick}
                    sx={{ width: '100%', py: 1, mb: 2 }}>
                    {loadingPrintable ? <CircularProgress size={24} color="inherit" sx={{ mr: 1 }} /> : <Icon sx={{ mr: 1 }}>cloud_download</Icon>}
                    {t('page.view.found.download.printable')}
                </Button>
            </span>
            <span
                title={noImprimible ? t('page.view.found.download.notavailable') : undefined}
                style={!isSmallScreen ? { marginRight: '16px' } : undefined}>
                <Button
                    variant="contained"
                    disabled={noImprimible}
                    onClick={handlePreviewClick}
                    endIcon={<Icon sx={{ mr: 1 }}>{previewShow ? 'expand_less' : 'expand_more'}</Icon>}
                    sx={{ width: '100%', py: 1, mb: 2 }}>
                    {loadingPreview ? <CircularProgress size={24} color="inherit" sx={{ mr: 1 }} /> : <Icon sx={{ mr: 1 }}>preview</Icon>}
                    {t('page.view.found.download.preview')}
                </Button>
            </span>
        </Box>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, height: '100%', mt: 2 }}>
            <Box sx={{ display: 'flex', flexDirection: isSmallScreen ? 'column' : 'row', gap: 1, height: '100%', mt: 2 }}>
                <Preview
                    mainUrl={previewBaseUrl + '/' + data.hash + '/printable?preview=true&lang=' + currentLanguage}
                    fallbackUrl={previewBaseUrl + '/' + data.hash + '/original?preview=true&lang=' + currentLanguage}
                    show={previewShow}
                    onLoad={handlePreviewLoad}
                    onError={handlePreviewError}
                    printableFailed={printableFailed}
                    isSmallScreen />
                <Box sx={{ display: 'flex', flexDirection: 'column', width: (isSmallScreen || !previewShow) ? '100%' : '40%' }}>
                    <Firmants data={data} />
                </Box>
            </Box>
            <Divider sx={{ my: 2 }} />
            <MetadadesEni data={data} />
        </Box>
    </BasePage >;
}

const ViewNotFound: React.FC<any> = (props) => {
    const { t } = useTranslation();
    const { handleGoBack } = props;
    return <BasePage>
        <Box sx={{display: 'flex', flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 1,mt: 2}}>
            <Box sx={{ textAlign: 'center' }}>
                <Icon sx={{ fontSize: '81.92px', color: 'rgba(200, 0, 0, 0.5)' }}>highlight_off</Icon>
            </Box>
            <Typography variant="h4" sx={{ textAlign: 'center', alignSelf: 'center'}}>
                {t('page.view.notFound.title')}
            </Typography>
        </Box>
        <Typography variant="h6" gutterBottom sx={{ textAlign: 'center', mb: 3 }}>
            {t('page.view.notFound.description')}
        </Typography>
        <Box sx={{ display: 'flex', justifyContent: 'center', width: '100%', mt: 2 }}>
            <Button variant="contained" onClick={handleGoBack} sx={{ py: 1 }}>
                <Icon sx={{ mr: 1 }}>arrow_back</Icon> {t('page.view.notFound.back')}
            </Button>
        </Box>
    </BasePage>;
}

const useCsvParam = () => {
    const { csv: csvParam } = useParams();
    const [searchParams] = useSearchParams();
    const csvSearchParam = searchParams?.get('hash');
    return csvParam ?? csvSearchParam;
}

const View: React.FC = () => {
    const { goBack } = useAppContext();
    const {
      isReady: apiIsReady,
      request: apiRequest,
      currentState: apiCurrentState,
    } = useResourceApiService("document");
    const csv = useCsvParam();
    const [loading, setLoading] = React.useState<boolean>(true);
    const [loadingOriginal, setLoadingOriginal] =
      React.useState<boolean>(false);
    const [loadingPrintable, setLoadingPrintable] =
      React.useState<boolean>(false);
    const [printableFailed, setPrintableFailed] = React.useState(false);
    const [data, setData] = React.useState<any>();
    const [error, setError] = React.useState<any>();
    const { currentLanguage } = useAppContext();
    const lang = currentLanguage;

    React.useEffect(() => {
      if (apiIsReady) {
        setLoading(true);
        apiRequest("getOne", null, { data: { csv }, refresh: true })
          .then((state) => {
            setData(state.data);
            setError(undefined);
          })
          .catch((error) => {
            setData(null);
            if (error.status !== 404) {
              setError(error);
            }
          })
          .finally(() => setLoading(false));
      }
    }, [csv, apiIsReady]);

    const saveState = (state: any) => {
      const contentDispositionHeader = state.headers.get("Content-Disposition");
      const fileNameIndex = contentDispositionHeader.indexOf("filename=");
      const fileName =
        fileNameIndex !== -1
          ? contentDispositionHeader.substring(
              fileNameIndex + "filename=".length
            )
          : undefined;
      saveAs(state.data, fileName);
    };

    const handleOriginalClick = () => {
      setLoadingOriginal(true);
      apiRequest("downloadOriginal", null, { data: { csv }, refresh: true })
        .then((state) => {
          saveState(state);
        })
        .catch((error) => {
          console.error(error);
        })
        .finally(() => setLoadingOriginal(false));
    };

    const handlePrintableClick = () => {
      setLoadingPrintable(true);
      apiRequest("downloadPrintable", null, {
        data: { csv, lang },
        refresh: true,
      })
        .then((state) => {
          saveState(state);
        })
        .catch((error) => {
          console.error(error);
          setPrintableFailed(true);
        })
        .finally(() => setLoadingPrintable(false));
    };

    if (loading) {
      return <ViewLoading />;
    } else if (error != null) {
      return <ViewError error={error} handleGoBack={() => goBack("/")} />;
    } else if (data != null) {
      return (
        <ViewFound
          data={data}
          loadingOriginal={loadingOriginal}
          loadingPrintable={loadingPrintable}
          handleOriginalClick={handleOriginalClick}
          handlePrintableClick={handlePrintableClick}
          previewBaseUrl={apiCurrentState?.links?.defaultContext}
          printableFailed={printableFailed}
          setPrintableFailed={setPrintableFailed}
        />
      );
    } else {
      return <ViewNotFound handleGoBack={() => goBack("/")} />;
    }
}

export default View;