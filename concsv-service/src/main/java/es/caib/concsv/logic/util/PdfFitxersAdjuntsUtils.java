package es.caib.concsv.logic.util;

import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfCatalog;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import es.caib.concsv.logic.intf.model.DocumentInfo;
import lombok.extern.slf4j.Slf4j;

/**
 * Detecta els PDF que contenen altres fitxers (issue #18). La còpia autèntica imprimible només
 * copia les pàgines del PDF i, per tant, en perdria aquests fitxers:
 * <ul>
 * <li><b>Portafoli</b> (<i>PDF Portfolio</i>, ISO 32000-1 §12.3.5): el catàleg té l'entrada
 * {@code /Collection} i el lector mostra la llista de documents. La pàgina del PDF sol ser només
 * una portada.</li>
 * <li><b>Fitxers adjunts</b>: PDF normal amb fitxers a {@code /Names/EmbeddedFiles}, a l'entrada
 * {@code /AF} del catàleg (PDF/A-3) o en anotacions {@code /FileAttachment} de les pàgines; per
 * exemple, els certificats energètics amb l'XML incrustat.</li>
 * </ul>
 *
 * @author Límit Tecnologies
 */
@Slf4j
public final class PdfFitxersAdjuntsUtils {

	private PdfFitxersAdjuntsUtils() {
	}

	/**
	 * Informa {@code portafoli} i {@code fitxersAdjunts} del document. No llança excepcions: si el
	 * PDF no es pot analitzar, els camps queden sense informar i el document es tracta com fins ara.
	 */
	public static void detecta(PdfDocument pdfDoc, DocumentInfo documentInfo) {
		if (pdfDoc == null || documentInfo == null) {
			return;
		}
		try {
			boolean portafoli = esPortafoli(pdfDoc);
			boolean fitxersAdjunts = portafoli || teFitxersAdjunts(pdfDoc);
			documentInfo.setPortafoli(portafoli);
			documentInfo.setFitxersAdjunts(fitxersAdjunts);
			if (fitxersAdjunts) {
				log.debug(
						"\tDocument {}: portafoli={}, fitxers incrustats={}",
						documentInfo.getCodi(),
						portafoli,
						pdfDoc.getCatalog().getNameTree(PdfName.EmbeddedFiles).getNames().size());
			}
		} catch (Exception ex) {
			log.warn(
					"No s'ha pogut comprovar si el document {} és un portafoli o té fitxers adjunts: {}",
					documentInfo.getCodi(),
					ex.toString());
		}
	}

	/** Indica si el PDF és un portafoli: el catàleg té el diccionari {@code /Collection}. */
	public static boolean esPortafoli(PdfDocument pdfDoc) {
		return pdfDoc.getCatalog().getPdfObject().getAsDictionary(PdfName.Collection) != null;
	}

	/** Indica si el PDF conté algun fitxer adjunt, en qualsevol de les formes que preveu l'estàndard. */
	public static boolean teFitxersAdjunts(PdfDocument pdfDoc) {
		PdfCatalog catalog = pdfDoc.getCatalog();
		if (!catalog.getNameTree(PdfName.EmbeddedFiles).getNames().isEmpty()) {
			return true;
		}
		PdfArray fitxersAssociats = catalog.getPdfObject().getAsArray(PdfName.AF);
		if (fitxersAssociats != null && !fitxersAssociats.isEmpty()) {
			return true;
		}
		for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
			PdfArray anotacions = pdfDoc.getPage(i).getPdfObject().getAsArray(PdfName.Annots);
			if (anotacions == null) {
				continue;
			}
			for (int j = 0; j < anotacions.size(); j++) {
				PdfDictionary anotacio = anotacions.getAsDictionary(j);
				if (anotacio != null && PdfName.FileAttachment.equals(anotacio.getAsName(PdfName.Subtype))) {
					return true;
				}
			}
		}
		return false;
	}

}
