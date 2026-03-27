package es.caib.concsv.ejb.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import com.itextpdf.kernel.exceptions.BadPasswordException;
import com.itextpdf.kernel.exceptions.PdfException;
import es.caib.concsv.service.model.DocumentInfo;
import org.apache.log4j.Logger;

import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.kernel.pdf.PdfDate;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.signatures.CertificateInfo;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.SignatureUtil;

import es.caib.concsv.service.model.DocumentSigner;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.util.Store;


/**
 * Utilitat per treballar amb documents PDF signats.
 * Permet extreure informació sobre les signatures digitals presents en un PDF.
 */
public class PdfSignerUtils {
	Logger log = Logger.getLogger(this.getClass());
	
	byte[] pdfDocument = null;
	SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss 'GMT'Z");

	public PdfSignerUtils(byte[] pdfDocument) {
		this.pdfDocument = pdfDocument;
	}

    /**
     * Extreu la informació de totes les signatures presents en el PDF i construeix una llista
     * de {@link DocumentSigner} amb informació rellevant.
     * Si no hi ha signatures retorna una llista buida.
     */
	public ArrayList<DocumentSigner> getPdfSigners(DocumentInfo documentInfo) throws IOException, BadPasswordException {
		ArrayList<DocumentSigner> listSigners = new ArrayList<DocumentSigner>();
		try {
			PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfDocument));
			reader.setUnethicalReading(true); // problemes amb el desencriptat d'algunes parts del document
			PdfDocument pdfDoc = new PdfDocument(reader);
			PdfAcroForm form = PdfAcroForm.getAcroForm(pdfDoc, false);
			if (form == null) {
				log.debug("\tDocument sense formulari (d'on obtenim les firmes).");
				return listSigners;
			}
			SignatureUtil signUtil = new SignatureUtil(pdfDoc);
			List<String> names = signUtil.getSignatureNames();
			PdfPKCS7 pk;
			for (String name : names) {
				try {
					PdfDictionary v = signUtil.getSignatureDictionary(name);
					if (v != null) {
						PdfName e = v.getAsName(PdfName.SubFilter);
						if (e.equals(PdfName.ETSI_CAdES_DETACHED) || e.equals(PdfName.Adbe_pkcs7_sha1)) {
							pk = signUtil.readSignatureData(name);
							X509Certificate cert = (X509Certificate) pk.getSigningCertificate();
							if (cert != null) {
								log.debug("Camps del certificat del PDF:");
								for (String key: CertificateInfo.getSubjectFields(cert).getFields().keySet()) {
									log.debug(key + ": " + CertificateInfo.getSubjectFields(cert).getField(key));
								}
								String surname = CertificateInfo.getSubjectFields(cert).getField("SURNAME");
								String givenName = CertificateInfo.getSubjectFields(cert).getField("GIVENNAME");
								String serial = CertificateInfo.getSubjectFields(cert).getField("SN");
								String organization = CertificateInfo.getSubjectFields(cert).getField("O");
								String signerName = "";
								DocumentSigner signer = new DocumentSigner();
								if (surname == null && givenName == null) {
									signer.setSignerO(organization);
								} else {
									if (surname == null) surname = "";
									if (givenName == null) givenName = "";
									signerName = givenName + " " + surname;
									signer.setSignerCN(signerName);
									signer.setSignerOU2(organization);
									signer.setIdEuropeu(serial);
								}
								PdfString strSignDate = v.getAsString(PdfName.M);
								if (strSignDate != null && PdfDate.decode(strSignDate.toString()) != null) {
									signer.setDataSignatura(sdf.format(PdfDate.decode(strSignDate.toString()).getTime()));
								}
								if (pk.getSignDate() != null) {
									signer.setDataSegell(sdf.format(pk.getSignDate().getTime()));
								}
								PdfString strReason = v.getAsString(PdfName.Reason);
								if (strReason != null) {
									signer.setReason(strReason.toUnicodeString());
								}
								listSigners.add(signer);
							}
						}
					}
				} catch(Throwable th) {
					log.error("Error obtenint la informació de la firma " + name + ": " + th.getMessage(), th);
				}
			}
		} catch (PdfException pdfEx) {
			documentInfo.setMalformatted(true);
			listSigners = getPdfSignersByPdfBox();
		}
		return listSigners;
	}

	protected ArrayList<DocumentSigner> getPdfSignersByPdfBox() throws IOException {
		ArrayList<DocumentSigner> listSigners = new ArrayList<>();
		try (PDDocument doc = Loader.loadPDF(pdfDocument)) {
			for (PDSignature sig : doc.getSignatureDictionaries()) {
				DocumentSigner signer = new DocumentSigner();
				signer.setReason(sig.getReason());
				Calendar signDate = sig.getSignDate();
				if (signDate != null) {
					signer.setDataSignatura(sdf.format(signDate.getTime()));
				}
				signer.setSignerCN(sig.getName());
				signer.setSignerO(sig.getLocation());
				// Extreure el contingut binari de la signatura PKCS7
				byte[] contents = sig.getContents(pdfDocument);
				if (contents != null && contents.length > 0) {
					try {
						// Parsejar el bloc PKCS7 per obtenir certificats
						CMSSignedData cms = new CMSSignedData(contents);
						SignerInformationStore signers = cms.getSignerInfos();
						Store<X509CertificateHolder> certStore = cms.getCertificates();
						for (SignerInformation signerInfo : signers.getSigners()) {
							Collection<X509CertificateHolder> certCollection = certStore.getMatches(signerInfo.getSID());
							for (X509CertificateHolder holder : certCollection) {
								X509Certificate cert = new JcaX509CertificateConverter()
										.getCertificate(holder);
								signer.setSignerCN(cert.getSubjectX500Principal().getName());
								signer.setSignerOU(cert.getIssuerX500Principal().getName());
								signer.setTipoCertificat(cert.getSigAlgName());
								signer.setDataSegell(sdf.format(cert.getNotBefore()));
								signer.setIdEuropeu(cert.getSerialNumber().toString());
								// Extraure més camps del subjecte:
								X500Name x500name = new X500Name(cert.getSubjectX500Principal().getName());
								RDN[] oFields = x500name.getRDNs(BCStyle.O);
								if (oFields.length > 0) {
									signer.setSignerO(IETFUtils.valueToString(oFields[0].getFirst().getValue()));
								}
								RDN[] cnFields = x500name.getRDNs(BCStyle.CN);
								if (cnFields.length > 0) {
									signer.setSignerCN(IETFUtils.valueToString(cnFields[0].getFirst().getValue()));
								}
							}
						}
					} catch (Exception e) {// No donem error si no es pot analitzar el PKCS7
						log.debug("El certificat d'una signatura PDFBox no s'ha pogut extreure: " + e.getMessage());
					}
				}
				listSigners.add(signer);
			}
		}
		return listSigners;
	}

}
