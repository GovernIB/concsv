package es.caib.concsv.logic.util;

import com.sun.xml.messaging.saaj.util.ByteInputStream;
import es.caib.comanda.ms.salut.helper.IntegracioApp;
import es.caib.concsv.commons.config.PropertyFileConfigUtil;
import es.caib.concsv.logic.helper.IntegracionsHelper;
import es.caib.concsv.logic.intf.config.PropertyConfig;
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
			this.integracionsHelper.addSuccessOperation(IntegracioApp.CDO, System.currentTimeMillis() - t0,
				"Càrrega de formats convertibles", null, null);
		}
		if (mimeByExtension.isEmpty()) {
			ConversionDocumentException ex = new ConversionDocumentException("La llista de formats convertibles es buida.");
			this.integracionsHelper.addErrorOperation(IntegracioApp.CDO, System.currentTimeMillis() - t0,
				"Càrrega de formats convertibles", null, null, ex);
			throw ex;
		}
	}

	public byte[] convertToPdf(byte[] content, String extension) throws InputDocumentNotSupportedException,
			OutputDocumentNotSupportedException, ConversionDocumentException {
		long t0 = System.currentTimeMillis();
		Throwable error = null;
		byte[] ret = null;
		var prop = PropertyFileConfigUtil.getProperties();
		IDocumentConverterPlugin oodcp = new OpenOfficeDocumentConverterPlugin(PropertyConfig.PROP_BASE_PREFIX_DOCUMENT_CONVERTER, prop);
		ByteArrayInputStream is = new ByteInputStream(content, content.length);
		ByteArrayOutputStream os = null;
		try {
			os = new ByteArrayOutputStream();
			oodcp.convertDocumentByMime(is, mimeByExtension.get(extension), os, mimeByExtension.get("pdf"));
			ret = os.toByteArray();
		} catch (Exception ex) {
			error = ex;
			throw ex;
		} finally {
			if (os != null) {
				try { os.close(); } catch (Exception e) {;}
			}
			integracionsHelper.addOperation(IntegracioApp.CDO, t0, "Conversió de document a PDF", endpoint(),
				IntegracionsHelper.params("extensio", extension, "mida", String.valueOf(content.length)), error);
		}
		return ret;
	}

	/** Adreça (host:port) del servidor OpenOffice, per al monitor d'integracions; null si no és configurada. */
	private static String endpoint() {
		try {
			var prop = PropertyFileConfigUtil.getProperties();
			String host = prop.getProperty(PropertyConfig.PROP_BASE_PREFIX_DOCUMENT_CONVERTER + "plugins.documentconverter.openoffice.host");
			String port = prop.getProperty(PropertyConfig.PROP_BASE_PREFIX_DOCUMENT_CONVERTER + "plugins.documentconverter.openoffice.port");
			return host == null ? null : (port == null ? host : host + ":" + port);
		} catch (Exception ex) {
			return null;
		}
	}

	public boolean isExtensionSupported(String fileExtension) throws ConversionDocumentException {
		return mimeByExtension.containsKey(fileExtension);
	}

	public String getMimeByExtension(String fileExtension) {
		return mimeByExtension.get(fileExtension);
	}
}
