package es.caib.concsv.ejb.plugins;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Properties;

import org.fundaciobit.plugins.validatesignature.afirmacxf.AfirmaCxfValidateSignaturePlugin;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.PdfPKCS7;
import com.itextpdf.signatures.SignatureUtil;

/** Implementacio del plugin de validació de firmes. */
public class ValidateSignaturePlugin extends AfirmaCxfValidateSignaturePlugin {

    public ValidateSignaturePlugin() {
        super();
    }

    public ValidateSignaturePlugin(String propertyKeyBase, Properties properties) {
        super(propertyKeyBase, properties);
    }

    public ValidateSignaturePlugin(String propertyKeyBase) {
        super(propertyKeyBase);
    }
    
    /** Sobrescriptura del mètode del plugin 
     * per no emprar la llibreria anterior de iText5. */
    @Override
    protected boolean containsTimeStamp(byte[] signature) {
        try {
        	PdfReader reader = new PdfReader(new ByteArrayInputStream(signature));
    		PdfDocument pdfDoc = new PdfDocument(reader);
    		SignatureUtil signUtil = new SignatureUtil(pdfDoc);
    		List<String> names = signUtil.getSignatureNames();
            String signatureName = names.get(names.size() - 1);
            PdfPKCS7 pkcs7 = signUtil.readSignatureData(signatureName);
            if (pkcs7.getTimeStampDate() != null && pkcs7.getTimeStampTokenInfo() != null) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return false;
    }

}
