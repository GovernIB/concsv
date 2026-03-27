package es.caib.concsv.ejb.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import es.caib.comanda.model.v1.salut.IntegracioApp;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fundaciobit.plugins.validatesignature.api.IValidateSignaturePlugin;
import org.fundaciobit.plugins.validatesignature.api.SignatureDetailInfo;
import org.fundaciobit.plugins.validatesignature.api.SignatureRequestedInformation;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureRequest;
import org.fundaciobit.plugins.validatesignature.api.ValidateSignatureResponse;
import org.fundaciobit.plugins.validatesignature.api.ValidationStatus;
import org.fundaciobit.pluginsib.validatecertificate.InformacioCertificat;

import es.caib.concsv.commons.config.PropertyFileConfigUtil;
import es.caib.concsv.ejb.plugins.ValidateSignaturePlugin;
import es.caib.concsv.service.model.DocumentSigner;

@Slf4j
@RequiredArgsConstructor
public class ValidacioFirmaUtils {
	
	private final static String valideProp = "es.caib.concsv.forceValideCert";
	
	byte[] documentData = null;
	byte[] signatureData = null;

	private final String identificacio;
	private final IntegracionsHelper integracionsHelper;
	
	public void setDocument(byte[] documentData) {
		this.documentData = documentData;
	}
	public void setSignatura(byte[] signatureData) {
		this.signatureData = signatureData;
	}

	public List<DocumentSigner> validaFirma() throws Exception {
		boolean isError = true;
		long t0 = System.currentTimeMillis();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss 'GMT'Z", Locale.getDefault());
			Properties prop = PropertyFileConfigUtil.getProperties();
			Boolean valideCert = (prop.get(valideProp) == null || "false".equalsIgnoreCase((String) prop.get(valideProp))) ? false : true;
			IValidateSignaturePlugin plugin;
			plugin = new ValidateSignaturePlugin("es.caib.concsv.", prop);
			ValidateSignatureRequest vsr = new ValidateSignatureRequest();
			vsr.setLanguage("ca");
			if (signatureData == null) {
				vsr.setSignatureData(documentData);
			} else {
				vsr.setSignatureData(signatureData);
				vsr.setSignedDocumentData(documentData);
			}
			SignatureRequestedInformation signatureRequestedInformation = new SignatureRequestedInformation();
			signatureRequestedInformation.setReturnCertificateInfo(true);
			signatureRequestedInformation.setReturnTimeStampInfo(true);
			signatureRequestedInformation.setReturnCertificates(true);
			vsr.setSignatureRequestedInformation(signatureRequestedInformation);
			ValidateSignatureResponse response = plugin.validateSignature(vsr);
			if (response != null) {
				ValidationStatus statusInfo = response.getValidationStatus();
				final int status = statusInfo.getStatus();
				ArrayList<DocumentSigner> result = new ArrayList<>();
				if (!valideCert || status == ValidationStatus.SIGNATURE_VALID) {
					for (SignatureDetailInfo sigDetail : response.getSignatureDetailInfo()) {
						//log.debug(ToStringBuilder.reflectionToString(sigDetail,ToStringStyle.SHORT_PREFIX_STYLE));
						//log.debug(ToStringBuilder.reflectionToString(sigDetail.getTimeStampInfo(),ToStringStyle.SHORT_PREFIX_STYLE));
						DocumentSigner docSigner = new DocumentSigner();
						InformacioCertificat certInfo = sigDetail.getCertificateInfo();
						docSigner.setIdEuropeu(certInfo.getIdEuropeu());
						docSigner.setSistemaComponent(certInfo.getDenominacioSistemaComponent());
						docSigner.setSignerCN(certInfo.getNomCompletResponsable());
						docSigner.setPuesto(certInfo.getLlocDeFeina());
						docSigner.setSignerOU(certInfo.getUnitatOrganitzativa()); //certificado electronico empleado publico
						docSigner.setSignerO(certInfo.getOrganitzacio());
						docSigner.setTipoCertificat(classificacioToString(certInfo.getClassificacio()));
						if (certInfo.getClassificacio() == 11 || certInfo.getClassificacio() == 12) {
							docSigner.setRepresentat(true);
							docSigner.setRepresentatRaoSocial(certInfo.getRaoSocial());
							docSigner.setRepresentatNifCif(certInfo.getUnitatOrganitzativaNifCif());
							docSigner.setRepresentatDocument(certInfo.getDocumentRepresentacio());
						}
						//docSigner.setSignerOU2(certInfo.getEmissorOrganitzacio());
						// Mirar quin triam data del segell (pdf / @firma) ho podem deixar pels que no son pdf
						if (sigDetail.getTimeStampInfo() != null && sigDetail.getTimeStampInfo().getCreationTime() != null) {
							docSigner.setDataSegell(sdf.format(sigDetail.getTimeStampInfo().getCreationTime().getTime()));
						}
						result.add(docSigner);
					}
					isError = false;
					return result;
				} else {
					throw new Exception("Error de validació de certificat: " + statusInfo.getErrorMsg());
				}
			} else {
				throw new Exception("El sistema de validació de firma ha retornat una resposta <null>");
			}
		} finally {
			long durada = System.currentTimeMillis() - t0;
			integracionsHelper.addOperation(IntegracioApp.VFI, t0, isError);
			log.trace("Validació de firmes del document " + identificacio + ": " + (isError ? "ERROR" : "OK") + " (" + (durada) + " ms)");
		}
	}
	
	private String classificacioToString(int c) {
		switch (c) {
	      case InformacioCertificat.CLASSIFICACIO_NO_QUALIFICATS:
	        return "COMPONENTS";
	      case InformacioCertificat.CLASSIFICACIO_PERSONA_FISICA:
	    	  return "PERSONA FISICA";
	      case InformacioCertificat.CLASSIFICACIO_PERSONA_JURIDICA:
	    	  return "PERSONA JURIDICA";
	      case InformacioCertificat.CLASSIFICACIO_SEU_ELECTRONICA:
	    	  return "Sede según la ley 40/2015 (no cualificado)";
	      case InformacioCertificat.CLASSIFICACIO_SEGELL:
	    	  return "Sello según la ley 40/2015 (no cualificado)";
	      case InformacioCertificat.CLASSIFICACIO_EMPLEAT_PUBLIC:
	        return "Empleado Público según la ley 40/2015";
	      case InformacioCertificat.CLASSIFICACIO_ENTITAT_SENSE_PERSONALITAT_JURIDICA:
	        return "Entidad sin personalidad jurídica (no cualificado)";
	      case InformacioCertificat.CLASSIFICACIO_EMPLEAT_PUBLIC_AMB_PSEUDONIM:
	        return "Empleado público con seudónimo según el RD 1671/2009";
	      case InformacioCertificat.CLASSIFICACIO_SEGELL_QUALIFICAT:
	        return "Cualificado de sello, según el reglamente UE 910/2014";
	      case InformacioCertificat.CLASSIFICACIO_AUTENTIFICACIO_QUALIFICAT:
	        return "Cualificado de autenticación, según el reglamente UE 910/2014";
	      case InformacioCertificat.CLASSIFICACIO_SEGELL_DE_TEMPS:
	        return "Cualificado de servicio cualificado de sello de tiempo";
	      case InformacioCertificat.CLASSIFICACIO_AUTENTIFICACIO_REPRESENTANT_ADMINISTRACIO_PERSONA_JURIDICA:
	        return "Persona  física  representante  ante  las  Administraciones  Públicas de persona jurídica";
	      case InformacioCertificat.CLASSIFICACIO_AUTENTIFICACIO_REPRESENTANT_ADMINISTRACIO_ENTITAT:
	        return "Persona  física  representante  ante  las  Administraciones Públicas de entidad sin persona jurídica";
	      default:
	        return String.format("Tipus certificat desconegut (Tipus: %d)",c);
	    }
	}

}
