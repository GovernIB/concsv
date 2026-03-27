package es.caib.concsv.ejb.service.test.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import es.caib.concsv.service.model.DocumentInfo;
import org.junit.Test;

import es.caib.concsv.ejb.utils.PdfSignerUtils;
import es.caib.concsv.service.model.DocumentSigner;

/** Prova la classe PdfUtils.
 */
public class PdfUtilsTest {

	//@Test
	public void test() {
		String arxius [] = new String[] {
				"DIN1-1_signada_comentat", // #39 Formularis de la versió imprimible
				"434967_02_167_Declaracio v14_21csg_signed_comentat", // #27 Generació de la versió impresa d'un pdf amb formulari. Només de manipular ja perd info
				"url_amb_espais_comentat", // #23 Espais en blanc a les url de alguns visors. Necessita stamper.setAnnotationFlattening(true)
				"DOC_PROFORMA-1"
			};
		for (int i = 0; i < arxius.length; i++) {
			String arxiu = arxius[i];
			try {
                DocumentInfo documentInfo = new DocumentInfo();
				File file = new File("/tmp/concsv/" + arxiu + ".pdf");
				byte[] pdfSource = Files.readAllBytes(file.toPath());				
				PdfSignerUtils pdfUtil = new PdfSignerUtils(pdfSource);
				ArrayList<DocumentSigner> padesSignners = pdfUtil.getPdfSigners(documentInfo);
				System.out.println("Signers " + arxiu + ": " + padesSignners.size());
				int j = 1;
				for (DocumentSigner signer : padesSignners) {
					System.out.println("- Signer " + j++ + " " + arxiu + ": " + signer);
				}
			} catch(Exception e) {
				String errMsg = "Error provan l'arxiu " + arxiu + ": " + e.getMessage();
				System.err.println(errMsg);
				e.printStackTrace(System.err);
			}
		}
	}

	@Test
	public void test2() throws IOException {
		try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("blank_signed_x_3.pdf")) {
            DocumentInfo documentInfo = new DocumentInfo();
			byte[] pdfSource = inputStream.readAllBytes();
			PdfSignerUtils pdfUtil = new PdfSignerUtils(pdfSource);
			List<DocumentSigner> signers = pdfUtil.getPdfSigners(documentInfo);
			for (DocumentSigner signer: signers) {
				System.out.println("\t" + signer);
			}
		}
	}

}

