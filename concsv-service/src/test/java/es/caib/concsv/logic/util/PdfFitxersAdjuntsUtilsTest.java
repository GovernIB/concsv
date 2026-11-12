package es.caib.concsv.logic.util;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.annot.PdfFileAttachmentAnnotation;
import com.itextpdf.kernel.pdf.collection.PdfCollection;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import es.caib.concsv.logic.intf.model.DocumentInfo;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Detecció dels PDF portafoli i amb fitxers adjunts (#18). Els PDF es generen al test: els exemples
 * reals del client no es poden afegir al repositori (mida i dades personals).
 */
public class PdfFitxersAdjuntsUtilsTest {

	private enum Tipus { SIMPLE, ADJUNT, PORTAFOLI, ANOTACIO, FITXER_ASSOCIAT }

	@Test
	public void pdfSenseAdjunts() throws Exception {
		DocumentInfo info = detecta(genera(Tipus.SIMPLE));
		assertEquals(Boolean.FALSE, info.getPortafoli());
		assertEquals(Boolean.FALSE, info.getFitxersAdjunts());
	}

	@Test
	public void pdfAmbFitxerIncrustat() throws Exception {
		DocumentInfo info = detecta(genera(Tipus.ADJUNT));
		assertEquals(Boolean.FALSE, info.getPortafoli());
		assertEquals(Boolean.TRUE, info.getFitxersAdjunts());
	}

	@Test
	public void pdfPortafoli() throws Exception {
		DocumentInfo info = detecta(genera(Tipus.PORTAFOLI));
		assertEquals(Boolean.TRUE, info.getPortafoli());
		assertEquals(Boolean.TRUE, info.getFitxersAdjunts());
	}

	@Test
	public void pdfAmbAnotacioDeFitxerAdjunt() throws Exception {
		DocumentInfo info = detecta(genera(Tipus.ANOTACIO));
		assertEquals(Boolean.FALSE, info.getPortafoli());
		assertEquals(Boolean.TRUE, info.getFitxersAdjunts());
	}

	@Test
	public void pdfAmbFitxerAssociatAlCatalegSenseArbreDeNoms() throws Exception {
		DocumentInfo info = detecta(genera(Tipus.FITXER_ASSOCIAT));
		assertEquals(Boolean.FALSE, info.getPortafoli());
		assertEquals(Boolean.TRUE, info.getFitxersAdjunts());
	}

	/** PDF signat sense adjunts: la detecció no altera la lectura de les signatures. */
	@Test
	public void pdfSignatSenseAdjunts() throws Exception {
		byte[] pdf;
		try (InputStream is = getClass().getResourceAsStream("/blank_signed_x_3.pdf")) {
			assertNotNull(is);
			pdf = is.readAllBytes();
		}
		DocumentInfo info = new DocumentInfo();
		assertFalse(new PdfSignerUtils(pdf).getPdfSigners(info).isEmpty());
		assertEquals(Boolean.FALSE, info.getPortafoli());
		assertEquals(Boolean.FALSE, info.getFitxersAdjunts());
	}

	/** Si el PDF no es pot llegir amb iText, els camps queden sense informar. */
	@Test
	public void pdfMalFormatNoInformaElsCamps() throws Exception {
		DocumentInfo info = new DocumentInfo();
		try {
			new PdfSignerUtils("no és un PDF".getBytes(StandardCharsets.UTF_8)).getPdfSigners(info);
		} catch (Exception ignored) {
			// El fallback a PDFBox també falla: aquí només interessen els camps
		}
		assertNull(info.getPortafoli());
		assertNull(info.getFitxersAdjunts());
	}

	/** Detecta a través de PdfSignerUtils, que és el que fan els serveis de l'Arxiu i de la custòdia antiga. */
	private static DocumentInfo detecta(byte[] pdf) throws Exception {
		DocumentInfo info = new DocumentInfo();
		new PdfSignerUtils(pdf).getPdfSigners(info);
		return info;
	}

	private static byte[] genera(Tipus tipus) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos))) {
			PdfPage pagina = pdf.addNewPage();
			if (tipus == Tipus.SIMPLE) {
				return tanca(pdf, baos);
			}
			PdfFileSpec fitxer = PdfFileSpec.createEmbeddedFileSpec(
					pdf,
					"<metadades/>".getBytes(StandardCharsets.UTF_8),
					"Metadades",
					"metadades.xml",
					null,
					null,
					null);
			switch (tipus) {
				case ADJUNT:
					pdf.addFileAttachment("metadades.xml", fitxer);
					break;
				case PORTAFOLI:
					pdf.addFileAttachment("metadades.xml", fitxer);
					pdf.getCatalog().setCollection(new PdfCollection().setView(PdfCollection.DETAILS));
					break;
				case ANOTACIO:
					pagina.addAnnotation(new PdfFileAttachmentAnnotation(new Rectangle(36, 700, 20, 20), fitxer));
					break;
				case FITXER_ASSOCIAT:
					pdf.getCatalog().getPdfObject().put(PdfName.AF, new PdfArray(fitxer.getPdfObject()));
					break;
				default:
					break;
			}
			return tanca(pdf, baos);
		}
	}

	private static byte[] tanca(PdfDocument pdf, ByteArrayOutputStream baos) {
		pdf.close();
		return baos.toByteArray();
	}

}
