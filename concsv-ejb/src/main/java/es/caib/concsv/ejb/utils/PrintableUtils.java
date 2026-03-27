package es.caib.concsv.ejb.utils;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.forms.PdfAcroForm;
import com.itextpdf.forms.fields.merging.AddIndexStrategy;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.exceptions.BadPasswordException;
import com.itextpdf.kernel.geom.AffineTransform;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.kernel.utils.PdfAnnotationFlattener;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import es.caib.concsv.service.exception.DocumentNotExistException;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PrintableUtils {

	private static final int MARGIN_LEFT = 30;
	private static final int MARGIN_RIGTH = 25;
	private static final int MARGIN_TOP = 20;
	private static final int MARGIN_BOTTOM = 70;

	public int getNumPages(byte[] sourceDocument) throws IOException, BadPasswordException {
		PdfReader reader = new PdfReader(new ByteArrayInputStream(sourceDocument));
		reader.setUnethicalReading(true); // problemes amb el desencriptat d'algunes parts del document
		return new PdfDocument(reader).getNumberOfPages();
	}

	public byte[] imgToPrintablePdf(
			byte[] sourceDocument,
			PdfDocument metadataPdf,
			String documentInfoDownloadUrl,
			String documentInfoHash,
			String lang) throws IOException, GenericServiceException, DuplicatedHashException, DocumentNotExistException {
		File tempFile = null;
		try {
			tempFile = File.createTempFile("temp_" + System.currentTimeMillis(), ".tmp");
			PdfWriter writer = new PdfWriter(tempFile);
			PdfDocument docPdf = new PdfDocument(writer);
			PdfPage imgPage = docPdf.addNewPage(PageSize.A4);
			PdfCanvas pdfCanvas = new PdfCanvas(imgPage);
			ImageData imageData = ImageDataFactory.create(sourceDocument);
			Image image = new Image(imageData);
			image.scaleToFit(PageSize.A4.getWidth() - 60, PageSize.A4.getHeight() - 60);
			Canvas canvas = new Canvas(pdfCanvas,  PageSize.A4);
			canvas.add(image.setFixedPosition(30, PageSize.A4.getHeight() - image.getImageScaledHeight() - 40));
			canvas.close();
			Document document = new Document(docPdf);
			// Afegeix el recuadre al contingut escalat i rotat
			pdfCanvas = new PdfCanvas(imgPage);
			Rectangle rect = new Rectangle(
					PageSize.A4.getX() + 30,
					PageSize.A4.getY() + 70,
					PageSize.A4.getWidth() - 55,
					PageSize.A4.getHeight() - 90);
			pdfCanvas.setLineWidth(0.5f);
			pdfCanvas.rectangle(rect);
			pdfCanvas.stroke();
			// Crea la pàgina resum que pot tenir més de 1 pàgina
			int extraAddedPages = metadataPdf != null ? metadataPdf.getNumberOfPages() : 0;
			int totalPages = 1 + extraAddedPages;
			// Posa el peu de pàgina a la pàgina de la imatge
			setFooterPage(
					document,
					imgPage,
					documentInfoDownloadUrl,
					documentInfoHash,
					totalPages,
					1,
					lang);
			// Afegeix les pàgines resum al final i informa el peu de pàgina
			if (metadataPdf != null) {
				for (int i = 1; i <= extraAddedPages; i++) {
					PdfPage pageM = metadataPdf.getPage(i).copyTo(docPdf);
					pageM = docPdf.addPage(pageM);
					setFooterPage(
							document,
							pageM,
							documentInfoDownloadUrl,
							documentInfoHash,
							totalPages,
							i + 1,
							lang);
				}
				metadataPdf.close();
			}
			setDocumentProperties(docPdf, lang);
			docPdf.close();
			Path path = Paths.get(tempFile.getPath());
			return Files.readAllBytes(path);
		} finally {
			if (tempFile != null) {
				try {
					tempFile.delete();
				} catch (Exception e) {
					log.error("Error esborrant l'arxiu temporal " + tempFile.getAbsolutePath() + ": " + e.getMessage());
				}
			}
		}
	}

	public byte[] pdfToPrintablePdf(
			byte[] sourceDocument,
			PdfDocument metadataPdf,
			String documentInfoDownloadUrl,
			String documentInfoHash,
			String lang) throws IOException {
		File tempFile = File.createTempFile("temp_" + System.currentTimeMillis() + "_reduced", ".tmp");
		try {
			PdfWriter writer = new PdfWriter(tempFile);
			PdfReader reader = new PdfReader(new ByteArrayInputStream(sourceDocument));
			reader.setUnethicalReading(true); // problemes amb el desencriptat d'algunes parts del document
			PdfDocument srcPdf = new PdfDocument(reader);
			// Mira si cal estampar els comentaris
			boolean estampar = PdfAcroForm.getAcroForm(srcPdf, false) != null;
			if (estampar) {
				srcPdf.close();
				reader = new PdfReader(new ByteArrayInputStream(sourceDocument));
				PdfDocument pdfDoc = new PdfDocument(reader, writer);
				try {
					PdfAcroForm.getAcroForm(pdfDoc, false).flattenFields();
				} catch (StackOverflowError th) {
					log.warn("Error aplanant el formulari. Es provarà amb AddIndexStrategy :" + th.toString(), th);
					srcPdf.close();
					reader = new PdfReader(new ByteArrayInputStream(sourceDocument));
					pdfDoc = new PdfDocument(reader, writer);
					PdfAcroForm.getAcroForm(pdfDoc, false, new AddIndexStrategy()).flattenFields();
				}
				PdfAnnotationFlattener pdfAnnotationFlattener = new PdfAnnotationFlattener();
				pdfAnnotationFlattener.flatten(pdfDoc);
				new Document(pdfDoc).close();
				pdfDoc.close();
				// torna a carregar el document
				reader = new PdfReader(new ByteArrayInputStream(Files.readAllBytes(tempFile.toPath())));
				reader.setUnethicalReading(true); // problemes amb el desencriptat d'algunes parts del document
				srcPdf = new PdfDocument(reader);
				writer = new PdfWriter(tempFile);
			}
			PdfDocument destPdf = new PdfDocument(writer);
			Document document = new Document(destPdf, destPdf.getDefaultPageSize(), true);
			int originalPages = srcPdf.getNumberOfPages();
			// Crea la pàgina resum per saber quantes pàgines ocuparà.
			int totalPages = originalPages + (metadataPdf != null ? metadataPdf.getNumberOfPages() : 0);
			// Tracta totes les pàgines
			PdfCanvas canvas;
			for (int i = 1; i <= originalPages; i++) {
				PdfPage origPage = srcPdf.getPage(i);
				Rectangle pageSize = origPage.getPageSizeWithRotation();
				PdfPage newPage = destPdf.addNewPage(new PageSize(pageSize.getWidth(), pageSize.getHeight()));
				// Informa el peu de pàgina
				setFooterPage(
						document,
						newPage,
						documentInfoDownloadUrl,
						documentInfoHash,
						totalPages,
						i,
						lang);
				// Copia el contingut escalat
				copyRotateScalePdf(origPage, newPage, destPdf);
				// Afegeix el recuadre al contingut escalat i rotat
				canvas = new PdfCanvas(newPage);
				pageSize = origPage.getPageSize();
				Rectangle rect = new Rectangle(
						newPage.getPageSize().getX(),
						newPage.getPageSize().getY(),
						pageSize.getWidth(),
						pageSize.getHeight());
				canvas.setLineWidth(0.5f);
				canvas.rectangle(rect);
				canvas.stroke();
				// Afegeix un salt de pàgina
				//document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
			}
			if (metadataPdf != null) {
				// Afegeix les pàgines resum al final i informa el peu de pàgina
				for (int i = 1; i <= metadataPdf.getNumberOfPages(); i++) {
					PdfPage pageM = metadataPdf.getPage(i).copyTo(destPdf);
					pageM = destPdf.addPage(pageM);
					setFooterPage(
							document,
							pageM,
							documentInfoDownloadUrl,
							documentInfoHash,
							totalPages,
							i + originalPages,
							lang);
				}
				metadataPdf.close();
			}
			setDocumentProperties(destPdf, lang);
			reader.close();
			destPdf.close();
			Path path = Paths.get(tempFile.getPath());
			return Files.readAllBytes(path);
		} finally {
			if (tempFile != null) {
				try {
					tempFile.delete();
				} catch (Exception e) {
					log.error("Error esborrant l'arxiu temporal " + tempFile.getAbsolutePath() + ": " + e.getMessage());
				}
			}
		}
	}

	private void copyRotateScalePdf(PdfPage origPage, PdfPage newPage, PdfDocument destDoc) throws IOException {
		Rectangle pageSize = origPage.getPageSizeWithRotation();
		AffineTransform transform = null;
		Float oWidth = pageSize.getWidth();
		Float oHeight = pageSize.getHeight();
		Float nscale = getScale(pageSize,
				oWidth - MARGIN_LEFT - MARGIN_RIGTH,
				oHeight - MARGIN_TOP - MARGIN_BOTTOM);
		Float scaledWidth = oWidth * nscale;
		Float scaledHeight = oHeight * nscale;
		Float offsetX = 0f;
		Float offsetY= 0f;
		switch(origPage.getRotation()) {
			case 0:
				log.debug("Sense rotar");
				offsetX = Math.max(0, (pageSize.getWidth() - MARGIN_RIGTH - MARGIN_LEFT - scaledWidth)) / 2;
				offsetY = Math.max(0, (pageSize.getHeight() - MARGIN_TOP - MARGIN_BOTTOM - scaledHeight)) / 2;
				transform = new AffineTransform(nscale, 0, 0, nscale,
						MARGIN_LEFT + offsetX,
						MARGIN_BOTTOM + offsetY); // Posició dels documents sense rotar
				break;
			case 90:
				log.debug("Rotat 90");
				transform = AffineTransform.getTranslateInstance(
						MARGIN_LEFT,
						pageSize.getHeight() - MARGIN_TOP);
				transform.rotate(-90*2*Math.PI/360);
				transform.scale(nscale, nscale);
				break;
			case 180:
				log.debug("Rotat 180");
				transform = new AffineTransform(nscale, 0, 0, nscale, MARGIN_LEFT + offsetX,
						MARGIN_BOTTOM + offsetY); // Posició dels documents rotats 180 graus
				AffineTransform rotate180 = new AffineTransform(-1f, 0, 0, -1f, scaledWidth, scaledHeight);
				transform.preConcatenate(rotate180);
				break;
			case 270:
				log.debug("Rotat 270");
				transform = AffineTransform.getTranslateInstance(
						pageSize.getWidth() - MARGIN_RIGTH,
						MARGIN_BOTTOM);
				transform.rotate(-270*2*Math.PI/360);
				transform.scale(nscale, nscale);
				break;
		}
		// Copia la pàgina i aplica la transformació de la possible rotació, escala i translació
		PdfCanvas canvas = new PdfCanvas(newPage);
		if (transform != null) {
			canvas.concatMatrix(transform);
		}
		PdfFormXObject pageCopy = origPage.copyAsFormXObject(newPage.getDocument());
		canvas.addXObjectAt(pageCopy, 0, 0);
		// Copia i transforma les annotations (enllaços)
		List<PdfAnnotation> annotations = origPage.getAnnotations();
		for (PdfAnnotation annotation: annotations) {
			PdfDictionary annotDict = (PdfDictionary)annotation.getPdfObject().copyTo(destDoc);
			PdfAnnotation copiedAnnotation = PdfAnnotation.makeAnnotation(annotDict);
			// Transforma el rectangle
			Rectangle rect = annotation.getRectangle().toRectangle();
			float[] coords = {
					rect.getLeft(), rect.getBottom(),
					rect.getRight(), rect.getTop()
			};
			transform.transform(coords, 0, coords, 0, 2);
			Rectangle newRect = new Rectangle(
					Math.min(coords[0], coords[2]),
					Math.min(coords[1], coords[3]),
					Math.abs(coords[2] - coords[0]),
					Math.abs(coords[3] - coords[1])
			);
			copiedAnnotation.setRectangle(new PdfArray(newRect));
			newPage.addAnnotation(copiedAnnotation);
		}
	}

	private void setFooterPage(
			Document document,
			PdfPage pdfPage,
			String documentInfoDownloadUrl,
			String documentInfoHash,
			Integer totalPages,
			Integer actualPage,
			String lang) throws IOException {
		Rectangle pageSize = pdfPage.getPageSize();
		String urlValidationLabel = "Aquesta és una còpia autèntica imprimible d'un document electrònic. Podeu comprovar la seva validesa al següent enllaç";
		String csvLabel = "CSV";
		String pageLabel = "Pàgina";
		String validationQRLabel = "QR de validació";
		if ("es".equals(lang)) {
			urlValidationLabel = "Esta es una copia auténtica imprimible de un documento electrónico. Puede comprobar su validez en el siguiente enlace";
			csvLabel = "CSV";
			pageLabel = "Página";
			validationQRLabel = "QR de validación";
		}
		// Generar QR
		BarcodeQRCode qrCode = new BarcodeQRCode(documentInfoDownloadUrl);
		PdfFormXObject qrCodeObject = qrCode.createFormXObject(ColorConstants.BLACK, document.getPdfDocument());
		Image qrCodeImage = new Image(qrCodeObject).setWidth(40).setHeight(40);
		float xQR = pageSize.getLeft() + 30;
		float yQR = pageSize.getBottom() + 26;
		qrCodeImage.setFixedPosition(xQR, yQR);
		PdfCanvas pdfCanvas = new PdfCanvas(pdfPage);
		Canvas canvas = new Canvas(pdfCanvas, pageSize);
		canvas.add(qrCodeImage);
		// Afegir informació
		Float xMeta = pageSize.getLeft() + 40;
		Float yMeta = pageSize.getBottom();
		float fontSize = 8f;
		// Adreça de validació:
		Paragraph p = new Paragraph (urlValidationLabel + ":")
				.setFontSize(fontSize)
				.setFixedPosition(actualPage, xMeta + 36, yMeta + 52, 800);
		canvas.add(p);
		// enllaç al ConCSV
		p = new Paragraph (documentInfoDownloadUrl)
				.setFontSize(fontSize)
				.setFixedPosition(actualPage, xMeta + 36, yMeta + 39, 600);
		canvas.add(p);
		// CSV
		p = new Paragraph (csvLabel + ": " + documentInfoHash)
				.setFontSize(fontSize)
				.setFixedPosition(actualPage, xMeta + 36, yMeta + 26, 600);
		canvas.add(p);
		// Número de pàgina actual / total
		p = new Paragraph (String.format("%s %d/%d", pageLabel, actualPage, totalPages))
				.setHorizontalAlignment(HorizontalAlignment.RIGHT)
				.setFontSize(fontSize)
				.setFixedPosition(actualPage, pageSize.getWidth() - 73, yMeta + 26, 600);
		canvas.add(p);
		canvas.close();
	}

	private void setDocumentProperties(PdfDocument doc, String lang) {
		String title;
		String locale;
		if (lang != null && "ca".equals(lang)) {
			title = "Còpia autèntica del document";
			locale = "ca-ES";
		} else {
			title = "Copia auténtica del documento";
			locale = "es-ES";
		}
		doc.getDocumentInfo().setTitle(title);
		doc.getCatalog().setLang(new PdfString(locale));
	}

	private static float getScale(Rectangle pagesize, float width, float height) {
		float scaleX = width / pagesize.getWidth();
		float scaleY = height / pagesize.getHeight();
		return Math.min(scaleX, scaleY);
	}

	Logger log = Logger.getLogger(this.getClass());

}
