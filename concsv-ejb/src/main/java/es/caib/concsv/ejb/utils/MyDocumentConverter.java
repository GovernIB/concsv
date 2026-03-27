package es.caib.concsv.ejb.utils;

import com.sun.xml.messaging.saaj.util.ByteInputStream;
import es.caib.comanda.model.v1.salut.IntegracioApp;
import es.caib.concsv.commons.config.PropertyFileConfigUtil;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import org.apache.log4j.Logger;
import org.fundaciobit.plugins.documentconverter.openoffice.OpenOfficeDocumentConverterPlugin;
import org.fundaciobit.pluginsib.documentconverter.ConversionDocumentException;
import org.fundaciobit.pluginsib.documentconverter.IDocumentConverterPlugin;
import org.fundaciobit.pluginsib.documentconverter.InputDocumentNotSupportedException;
import org.fundaciobit.pluginsib.documentconverter.OutputDocumentNotSupportedException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyDocumentConverter {
	private Logger log = Logger.getLogger(this.getClass());
	private static Map<String, String> mimeByExtension = null;
	private final IntegracionsHelper integracionsHelper;

	public MyDocumentConverter(IntegracionsHelper integracionsHelper) throws ConversionDocumentException{
		long t0 = System.currentTimeMillis();
		this.integracionsHelper = integracionsHelper;
		if (mimeByExtension == null) {
			mimeByExtension = new HashMap<String, String>();
			List<Object> objs = OpenOfficeDocumentConverterPlugin.DFR.getDocumentFormats();
			for (Object object : objs) {
				com.artofsolving.jodconverter.DocumentFormat df = (com.artofsolving.jodconverter.DocumentFormat) object;				
				mimeByExtension.put(df.getFileExtension(), df.getMimeType());
			}
			this.integracionsHelper.addSuccessOperation(IntegracioApp.ARX, System.currentTimeMillis() - t0);
		}
		if (mimeByExtension.isEmpty()) {
			this.integracionsHelper.addErrorOperation(IntegracioApp.ARX);
			throw new ConversionDocumentException("La llista de formats convertibles es buida.");
		}
	}

	public byte[] convertToPdf(byte[] content, String extension) throws InputDocumentNotSupportedException,
			OutputDocumentNotSupportedException, ConversionDocumentException {
		long t0 = System.currentTimeMillis();
		boolean hasError = true;
		byte[] ret = null;
		var prop = PropertyFileConfigUtil.getProperties();
		IDocumentConverterPlugin oodcp = new OpenOfficeDocumentConverterPlugin("es.caib.concsv.", prop);
		ByteArrayInputStream is = new ByteInputStream(content, content.length);
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			oodcp.convertDocumentByMime(is, mimeByExtension.get(extension), os, mimeByExtension.get("pdf"));
			ret = os.toByteArray();
			hasError = false;
		} finally {
			if (os != null) {
				try { os.close(); } catch (Exception e) {;}
			}
			integracionsHelper.addOperation(IntegracioApp.ARX, t0, hasError);
		}
		return ret;
	}
	
	public boolean isExtensionSupported(String fileExtension) throws ConversionDocumentException {
		return mimeByExtension.containsKey(fileExtension);
	}
	
	public String getMimeByExtension(String fileExtension) {
		return mimeByExtension.get(fileExtension);
	}
}
