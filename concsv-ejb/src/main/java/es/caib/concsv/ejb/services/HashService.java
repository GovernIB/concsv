
package es.caib.concsv.ejb.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

import com.itextpdf.kernel.exceptions.BadPasswordException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.itextpdf.barcodes.BarcodeQRCode;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;

import es.caib.concsv.ejb.annotation.ErrorInt;
import es.caib.concsv.ejb.annotation.PerformanceInt;
import es.caib.concsv.ejb.utils.MyDocumentConverter;
import es.caib.concsv.ejb.utils.OptionalMetadataBlock;
import es.caib.concsv.ejb.utils.PrintableUtils;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import es.caib.concsv.ejb.helpers.SubsistemesHelper;
import es.caib.concsv.service.enums.DocumentLocation;
import es.caib.concsv.service.enums.EniDocumentType;
import es.caib.concsv.service.enums.EniElaborationStatus;
import es.caib.concsv.service.enums.EniSignatureType;
import es.caib.concsv.service.exception.DocumentNotExistException;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.facade.HashServiceInterface;
import es.caib.concsv.service.facade.NewDigitalArchiveServiceInterface;
import es.caib.concsv.service.facade.OldSaveKeepingServiceInterface;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;
import es.caib.concsv.service.model.DocumentSigner;

@Slf4j
@ErrorInt
@PerformanceInt
@Stateless @Local({HashServiceInterface.class})
public class HashService implements HashServiceInterface {

	private final Float paragraphLeading = 8f;
	private final Float headerLeading = 203f;
	private final Float leftIdentation = 0f;

	private static final int MARGIN_LEFT = 30;
	private static final int MARGIN_RIGTH = 25;
	private static final int MARGIN_TOP = 20;
	private static final int MARGIN_BOTTOM = 70;

	@Inject @ConfigProperty(name = "es.caib.concsv.consult.oldSafeKeeping")
	private String consultOldSafeKeeping;
	@Inject @ConfigProperty(name = "es.caib.concsv.consult.newDigitalArchive")
	private String consultNewDigitalArchive;
	@Inject @ConfigProperty(name = "es.caib.concsv.logo.path", defaultValue = "")
	private String logoPath;
	/** Propietat amb el path cap al fitxer d'exclusions per CSV de documents. */
	@Inject @ConfigProperty(name = "es.caib.concsv.arxiu.documents.exclosos.path", defaultValue = "")
	private String exclusionsPath;
	/** Llista de CSV exclosos per a la descàrrega de l'original. */
	private List<String> csvExclosos = new ArrayList<String>();
	
	/** Indica si amagar per defecte el botó de descàrrega del botó original per documents amb versió imprimible. Per amagar posar el valor "true" */
	@Inject @ConfigProperty(name = "es.caib.concsv.amagar.boto.original", defaultValue = "false")
	private String amagarBotoOriginal;

	private PrintableUtils printableUtils = new PrintableUtils();

	@Inject
	private OldSaveKeepingServiceInterface oldSaveKeepingService;
	@Inject
	private NewDigitalArchiveServiceInterface newDigitalArchiveService;
	@Inject private SubsistemesHelper subsistemesHelper;
	@Inject private IntegracionsHelper integracionsHelper;

//	// Mètodes auxiliats per mostrar les traces de duració
//	private static ThreadLocal<Long> startTimeMillis = new ThreadLocal<>();
//	private void initDurationCalc() {
//		startTimeMillis.set(System.currentTimeMillis());
//		log.info("[CONSULTA HASH] Inici procés");
//	}
//	private void logDurationCalc(String desc) {
//		Long currentTimeMillis = System.currentTimeMillis();
//		log.info("[CONSULTA HASH] " + desc + " (" + (currentTimeMillis - startTimeMillis.get()) + "ms)");
//		startTimeMillis.set(currentTimeMillis);
//	}

	/** Si s'informa el document d'exclusions es llegeix i carrega.
	 * 
	 */
	@PostConstruct
	public void init() throws Exception {
		if (this.exclusionsPath != null && !this.exclusionsPath.isBlank() ) {
			long t0 = System.currentTimeMillis();
			try {
				File documentExclusions = new File(this.exclusionsPath);
				this.csvExclosos = Files.readAllLines(documentExclusions.toPath());
				log.info("Carregat el fitxer de documents exclosos \"" + this.getExclusionsPath() + "\" amb " + this.csvExclosos.size() + " línies.");
				subsistemesHelper.addSuccessOperation(SubsistemesHelper.SubsistemesEnum.EXC, System.currentTimeMillis() - t0);
			} catch (IOException e) {
				String errMsg = "Error llegint el fitxer de documents exclosos pel path \"" + this.getExclusionsPath() + "\": " + e.toString();
				log.error(errMsg, e);
				subsistemesHelper.addErrorOperation(SubsistemesHelper.SubsistemesEnum.EXC);
				throw new Exception(errMsg, e);
			}
		}
	}

	@PermitAll
	public DocumentInfo checkHash(final String hash)
			throws GenericServiceException, DuplicatedHashException, DocumentNotExistException {
		long t0 = System.currentTimeMillis();
		boolean hasError = true;
		try {
			DocumentInfo documentInfoNewDigitalArchive = null;
			DocumentInfo documentInfoOldSaveKeeping = null;

			// Consulta a l'Arxiu Digital CAIB
			documentInfoNewDigitalArchive = null;
			Throwable newDigitalArchiveException = null;
			if ("S".equals(consultNewDigitalArchive)) {
				boolean isError = false;
				long t00 = System.currentTimeMillis();
				try {
					documentInfoNewDigitalArchive = newDigitalArchiveService.checkHash(hash);
				} catch (Exception ex) {
					isError = true;
					if (ex instanceof ExecutionException) {
						newDigitalArchiveException = ex.getCause();
					} else {
						newDigitalArchiveException = ex;
					}
				} finally {
					log.trace("\tConsulta del hash " + hash + " a l'arxiu digital: " + (isError ? "ERROR" : "OK") + " (" + (System.currentTimeMillis() - t00) + " ms)");
				}
			}

			// Consulta a la custòdia antiga si no l'ha trobat a l'Arxiu
			Throwable oldSafeKeepingException = null;
			if (documentInfoNewDigitalArchive == null
					&& consultOldSafeKeeping != null
					&& "S".equals(consultOldSafeKeeping))
			{
				boolean isError = false;
				long t00 = System.currentTimeMillis();
				try {
					documentInfoOldSaveKeeping = oldSaveKeepingService.checkHash(hash);
					if (documentInfoOldSaveKeeping != null
							&& !documentInfoOldSaveKeeping.getCorrectSafeKeeping())
						throw new DocumentNotExistException();
				} catch (Exception ex) {
					isError = true;
					oldSafeKeepingException = ex;
					if (ex.getCause() instanceof DuplicatedHashException) {
						throw new DuplicatedHashException("Document duplicat a l'antiga custòdia!!!");
					} else if (ex instanceof DocumentNotExistException) {
						throw new DocumentNotExistException("El document no s'ha custodiat correctament.");
					}
				} finally {
					log.trace("\tConsulta del hash " + hash + " a l'antiga custòdia: " + (isError ? "ERROR" : "OK") + " (" + (System.currentTimeMillis() - t00) + " ms)");
				}
			}
			DocumentInfo docInfo = null;
			if (documentInfoOldSaveKeeping != null) {
				log.debug("\tHash " + hash + " trobat a l'antiga custòdia.");
				docInfo = documentInfoOldSaveKeeping;
			}
			if (documentInfoNewDigitalArchive != null) {
				log.debug("\tHash " + hash + " trobat a l'arxiu digital.");
				docInfo = documentInfoNewDigitalArchive;
			}
			if (docInfo != null) {
				try {
					if ("pdf".equalsIgnoreCase(docInfo.getExtensionFormato()) ||
						"jpg".equalsIgnoreCase(docInfo.getExtensionFormato())) {
						docInfo.setPrintable(true);
					} else {
						MyDocumentConverter dc = new MyDocumentConverter(integracionsHelper);
						docInfo.setPrintable(dc.isExtensionSupported(docInfo.getExtensionFormato()));
					}
				} catch (Exception ex) {
					throw new GenericServiceException("Error comprovant si el document " + hash + " és imprimible", ex);
				}
				docInfo.setCsvExclos(this.csvExclosos.contains(hash));
				docInfo.setAmagarBotoOriginal(this.isAmagarBotoOriginal());
			} else {
				log.debug("\tHash " + hash + " no trobat.");
				// Si no s'ha trobat i s'ha produit excepció la enregistra i propaga
				if (newDigitalArchiveException != null) {
					throwAndLogHashException(hash, newDigitalArchiveException);
				} else if (oldSafeKeepingException != null) {
					throwAndLogHashException(hash, oldSafeKeepingException);
				}
			}
			hasError = false;
			return docInfo;
		} finally {
			subsistemesHelper.addOperation(SubsistemesHelper.SubsistemesEnum.CHE, t0, hasError);
			log.debug("Consulta del hash " + hash + ": (" + (System.currentTimeMillis() - t0) + " ms)");
		}
	}
	
	/** Converteix el valor string a boolean. És true si el text del valor de la propietat és "true".
	 * 
	 * @return
	 */
	private boolean isAmagarBotoOriginal() {
		return this.amagarBotoOriginal != null 
				&& this.amagarBotoOriginal.trim().toLowerCase().equals("true");
	}

	@PermitAll
	public ArrayList<Entry<String, String>> getOptionalMetadata(String lang, DocumentInfo documentInfo){
		OptionalMetadataBlock optMetaBlk;
		boolean isError = false;
		long t0 = System.currentTimeMillis();
		try {
			optMetaBlk = new OptionalMetadataBlock();
			return optMetaBlk.getMetadataLabels(lang, documentInfo.getMetadata());
		} catch (Exception e) {
			isError = true;
			log.error("Error obtenint les metadades del document " + (documentInfo != null ? documentInfo.getHash() : "null") + ": " + e.getMessage(), e);
			return new ArrayList<Entry<String, String>>();
		} finally {
			subsistemesHelper.addOperation(SubsistemesHelper.SubsistemesEnum.MET, t0, isError);
		}
	}

    @PermitAll
	//@RolesAllowed({"CSV_REST"})
	public DocumentInfo checkHashFromUUID(final String uuid) throws DocumentNotExistException, GenericServiceException {
		boolean isError = true;
		long t0 = System.currentTimeMillis();
		try {
			DocumentInfo documentInfo = this.newDigitalArchiveService.checkHashFromUUID(uuid, null);
			documentInfo.setCsvExclos(this.getCsvExclosos().contains(documentInfo.getHash()));
			isError = false;
			return documentInfo;
		} catch (GenericServiceException ex) {
			log.error("Error al consultar document amb UUID: " + uuid, ex.getCause());
			throw ex;
		} finally {
			subsistemesHelper.addOperation(SubsistemesHelper.SubsistemesEnum.CHE, t0, isError);
			log.debug("Consulta del uuid " + uuid + ": " + (isError ? "ERROR" : "OK") + " (" + (System.currentTimeMillis() - t0) + " ms)");
		}
	}

	@PermitAll
	public DocumentContent getDocument(DocumentInfo documentInfo, Boolean packedFile) throws GenericServiceException {
		boolean isError = false;
		long t0 = System.currentTimeMillis();
		String identificador = documentInfo != null ?
				(DocumentLocation.NewDigitalArchive.equals(documentInfo.getDocumentLocation()) ?
						documentInfo.getDocumentCode() :
						documentInfo.getHash()) :
				"<null>";
		try {
			DocumentContent dc = null;
			if (documentInfo == null || documentInfo.getDocumentLocation() == null)
				return null;
			if (documentInfo.getDocumentLocation().equals(DocumentLocation.OldSaveKeeping)) {
				dc = this.oldSaveKeepingService.getDocument(documentInfo.getHash());
				if (!documentInfo.getDocumentName().toLowerCase().endsWith(".pdf")) 
					dc.setFileName(documentInfo.getDocumentName() + ".pdf");
				else
					dc.setFileName(documentInfo.getDocumentName());
			} else if (documentInfo.getDocumentLocation().equals(DocumentLocation.NewDigitalArchive)) {
				dc = this.newDigitalArchiveService.getDocument(documentInfo.getDocumentCode(), packedFile);
			}
			subsistemesHelper.addSuccessOperation(SubsistemesHelper.SubsistemesEnum.ORI, System.currentTimeMillis() - t0);
			return dc;
		} catch (Exception e) {
			isError = true;
			log.error("Error obtenint el document " + documentInfo.getHash() + ": " + e.getCause(), e);
			subsistemesHelper.addErrorOperation(SubsistemesHelper.SubsistemesEnum.ORI);
			throw new GenericServiceException(e);
		} finally {
			log.debug("Consulta del document " + identificador + ": " + (isError ? "ERROR" : "OK") + " (" + (System.currentTimeMillis() - t0) + " ms)");
		}
	}

    @PermitAll
	//@RolesAllowed({"CSV_REST"})
	public DocumentContent getEniDocument(DocumentInfo documentInfo) throws GenericServiceException {
		Boolean isError = false;
		long t0 = System.currentTimeMillis();
		String identificador = documentInfo != null ?
				(DocumentLocation.NewDigitalArchive.equals(documentInfo.getDocumentLocation()) ? documentInfo.getDocumentCode() : "<???>") :
				"<null>";
		try {
			DocumentContent dc = null;
			if (documentInfo == null || documentInfo.getDocumentLocation() == null) {
				isError = null;
				return null;
			}
			if (documentInfo.getDocumentLocation().equals(DocumentLocation.OldSaveKeeping)) {
				throw new GenericServiceException("No es un document ENI");
			} else if (documentInfo.getDocumentLocation().equals(DocumentLocation.NewDigitalArchive)) {
				dc = this.newDigitalArchiveService.getEniDocument(documentInfo.getDocumentCode());
			}
			return dc;
		} catch (Exception ex) {
			isError = true;
			log.error("Error obtenint les dades ENI del document " + (documentInfo != null ? documentInfo.getHash() : "null") + ": " + ex.getMessage(), ex);
			throw new GenericServiceException(ex);
		} finally {
			subsistemesHelper.addOperation(SubsistemesHelper.SubsistemesEnum.ENI, t0, isError);
			log.debug("Consulta del document ENI " + identificador + ": " + (isError ? "ERROR" : "OK") + " (" + (System.currentTimeMillis() - t0) + " ms)");
		}
	}

	@PermitAll
	public DocumentContent getPrintableDocument(DocumentInfo documentInfo, String lang) throws GenericServiceException {
		boolean isError = false;
		long t0 = System.currentTimeMillis();
		String identificador = documentInfo != null ? documentInfo.getHash() : "<null>";
		DocumentContent documentContent = null;
		try {
			if (documentInfo != null) {
				String fileExtension = documentInfo.getExtensionFormato().replace(".", "");
				documentContent = this.getDocument(documentInfo, false);
				log.debug("Document: " + documentInfo.getHash() + " MimeType: " + documentContent.getMimeType());
				if ("pdf".equalsIgnoreCase(fileExtension)) {
                    if (Boolean.TRUE.equals(documentInfo.getMalformatted())) {
                        throw new GenericServiceException("Aquest document PDF està mal format o conté errors i no es pot generar la còpia autèntica imprimible.", null);
                    }
					int numPages = printableUtils.getNumPages(documentContent.getContent());
					byte[] printableContent = printableUtils.pdfToPrintablePdf(
							documentContent.getContent(),
							getMetadataPdf(documentInfo, numPages, lang),
							documentInfo.getDownloadUrl(),
							documentInfo.getHash(),
							lang);
					documentContent.setContent(printableContent);
				} else if ("jpg".equalsIgnoreCase(fileExtension)) {
					byte[] printableContent = printableUtils.imgToPrintablePdf(
							documentContent.getContent(),
							getMetadataPdf(documentInfo, 1, lang),
							documentInfo.getDownloadUrl(),
							documentInfo.getHash(),
							lang);
					documentContent.setContent(printableContent);
				} else {
					MyDocumentConverter dconverter = new MyDocumentConverter(integracionsHelper);
					if (dconverter.isExtensionSupported(fileExtension)) {
						log.debug("Conversió " + fileExtension);
						byte[] pdfContent = dconverter.convertToPdf(documentContent.getContent(), fileExtension);
						int numPages = printableUtils.getNumPages(pdfContent);
						byte[] printableContent = printableUtils.pdfToPrintablePdf(
                            pdfContent,
							getMetadataPdf(documentInfo, numPages, lang),
							documentInfo.getDownloadUrl(),
							documentInfo.getHash(),
							lang);
						documentContent.setContent(printableContent);
					} else {
						throw new GenericServiceException(String.format("Conversio no soportada, csv:%s ext:%s ", documentInfo.getHash(), fileExtension));
					}
				}
				// El doc de "versio imprimible" es sempre un pdf
				documentContent.setMimeType("application/pdf");
				String fileName = documentInfo.getDocumentName();
				if (!fileName.toLowerCase().endsWith(".pdf")) {
					fileName += ".pdf";
				}
				// S'afegeix _imprimible com a "nom" + "_imprimible" + ".pdf"
				fileName = fileName.substring(0, fileName.lastIndexOf(".")) + "_imprimible.pdf";
				documentContent.setFileName(fileName);
			}
		} catch (BadPasswordException e) {
			isError = true;
			String errMsg = "El PDF está protegit amb contrasenya: " + documentInfo.getHash() + ": " + e.getMessage();
			documentInfo.setHasPassword(true);
			throw new GenericServiceException(errMsg, e);
		} catch (StackOverflowError th) {
			isError = true;
			String errMsg = "Error obtenint la versió imprimible del document " + documentInfo.getHash() + ": " + th.getMessage();
			throw new GenericServiceException(errMsg, th);
		} catch (Throwable th) {
			isError = true;
			String errMsg = "Error obtenint la versió imprimible del document " + documentInfo.getHash() + ": " + th.getMessage();
			log.error(errMsg, th);
			throw new GenericServiceException(errMsg, th);
		} finally {
			subsistemesHelper.addOperation(SubsistemesHelper.SubsistemesEnum.IMP, t0, isError);
			log.debug("Consulta del document imprimible " + identificador + ": " + (isError ? "ERROR" : "OK") + " (" + (System.currentTimeMillis() - t0) + " ms)");
        }
        return documentContent;
    }

	/** Crea un document en memòria i hi afegeix les pàgiens resum de metadades.
	 * @param totalPages 
	 * @throws IOException */
	private PdfDocument getMetadataPdf(DocumentInfo documentInfo, int totalPages, String lang) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfWriter writer = new PdfWriter(baos);
		PdfDocument metadataPdf = new PdfDocument(writer);
		Document documentM = new Document(metadataPdf);
		addMetadataPage(documentM, documentInfo, totalPages, lang);
		metadataPdf.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		PdfReader reader = new PdfReader(bais);
		metadataPdf = new PdfDocument(reader);
		return metadataPdf;
	}

	private PdfPage addMetadataPage(Document doc, DocumentInfo documentInfo, Integer totalPages, String lang)
			throws IOException {
		
		String electronicDocHeader = "DOCUMENT ELECTRÒNIC";
		String csvHeaderStr = "CODI SEGUR DE VERIFICACIÓ";
		String urlVerificationHeader = "ADREÇA DE VALIDACIÓ DEL DOCUMENT";
		String signerInfoHeader = "INFORMACIÓ DELS SIGNANTS";
		String metadataENIHeader = "METADADES ENI DEL DOCUMENT";
		String metadataHeader = "METADADES DEL DOCUMENT";
		String identifierLabel = "Identificador";
		String docNameLabel = "Nom del document";
		String organismLabel = "Òrgan";
		String adminLabel = "Administració";
		String citizenLabel = "Ciutadà";
		String originLabel = "Origen";
		String signerLabel = "Signant";
		String signerRepresentant = "(En representació de %s amb NIF / CIF %s)";
		String signatureDateLabel = "Data signatura";
		String advertenciaSignatura = "\"Data signatura\" és la data que tenia l'ordinador del signant en el moment de la signatura";
		String segellDateLabel = "Firma amb segell de temps";
		String docTypeLabel = "Tipus de document";
		String pagesLabel = "Pàgines";
		String ntiVersionLabel = "Versió NTI";
		String signatureTypeLabel = "Tipus de signatura";
		String elaborationStatusLabel = "Estat elaboració";
		String startDateLabel = "Data captura";
		String noteFootLabel = "Les evidències que garanteixen l'autenticitat, integritat i conservació a llarg termini del document es troben al gestor documental de la CAIB";
		String advertenciaComentaris ="ADVERTÈNCIA: Hi ha %d comentaris del document original que no s'han copiat a la versió impresa";
		String reasonLabel = "Raó";
        if ("es".equals(lang)) {
			electronicDocHeader = "DOCUMENTO ELECTRÓNICO";
			csvHeaderStr = "CÓDIGO SEGURO DE VERIFICACIÓN";
			urlVerificationHeader = "DIRECCIÓN DE VERIFICACIÓN DEL DOCUMENTO";
			signerInfoHeader = "INFORMACIÓN DE LOS FIRMANTES";
			metadataENIHeader = "METADATOS ENI DEL DOCUMENTO";
			metadataHeader = "METADATOS DEL DOCUMENTO";
			identifierLabel = "Identificador";
			docNameLabel = "Nombre del documento";
			organismLabel = "Órgano";
			adminLabel = "Administración";
			citizenLabel = "Ciudadano";
			signerRepresentant = "(En representación de %s con NIF / CIF %s)";
			originLabel = "Origen";
			signerLabel = "Firmante";
			signatureDateLabel = "Fecha firma: ";
			advertenciaSignatura = "\"Fecha de firma\" es la fecha que tenia el ordenador del firmante en el momento de la firma";
			segellDateLabel = "Firma con sello de tiempo";
			docTypeLabel = "Tipo de documento";
			pagesLabel = "Páginas";
			ntiVersionLabel = "Versión NTI";
			signatureTypeLabel = "Tipo de firma";
			elaborationStatusLabel = "Estado elaboración";
			startDateLabel = "Fecha captura";
			noteFootLabel = "Las evidencias que garantizan la autenticidad, integridad y conservación a llargo plazo del documento se encuentran en el gestor documental de la CAIB";
			advertenciaComentaris ="ADVERTENCIA: Hay %d comentarios del document original que no se han copiado a la versión impresa";
            reasonLabel = "Razón";
		}

		log.debug("Pàgina metadades");

		PdfPage page = doc.getPdfDocument().addNewPage(new PageSize(PageSize.A4));

		// Handler per comptar el número total de salts de pàgina
        CanviPaginaEventHandler canviPaginaHandler = new CanviPaginaEventHandler();
        doc.getPdfDocument().addEventHandler(PdfDocumentEvent.INSERT_PAGE, canviPaginaHandler);

		doc.setMargins(MARGIN_TOP, MARGIN_RIGTH, MARGIN_BOTTOM, MARGIN_LEFT);

		try {
			PdfCanvas pdfCanvas = new PdfCanvas(page);
			ImageData imageData = ImageDataFactory.create(logoPath);
			Image image = new Image(imageData);
			image.scaleToFit(150f, 150f);
			Canvas canvas = new Canvas(pdfCanvas,  page.getPageSize());
			canvas.add(image.setFixedPosition(20, PageSize.A4.getHeight() - image.getImageScaledHeight() - 30));
			canvas.close();
		} catch (IOException e) {
			log.error("Can't find image: " + logoPath);
		}
		
		// Estableix la tipografia
		doc.setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA));
		
		PdfFont defaultFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
		PdfFont defaultBoldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
		PdfFont urlFont = PdfFontFactory.createFont(StandardFonts.COURIER);

		// Paràgraf de capçalera
		Paragraph header = new Paragraph(electronicDocHeader)
				.setFontSize(18.0f)
				.setFont(defaultBoldFont)
				.setFontColor(new DeviceRgb(203, 2, 62))
				.setMarginTop(headerLeading)
				.setMarginBottom(10)
				.setFixedLeading(paragraphLeading)
				.setMarginLeft(leftIdentation);
		doc.add(header);
		

		setDocumnentParagraph(doc, csvHeaderStr, 9.0f, defaultBoldFont);

		setDocumnentParagraph(doc, documentInfo.getHash(), 9.0f, defaultFont);

		setDocumnentParagraph(doc, urlVerificationHeader, 9.0f, defaultBoldFont);

		setDocumnentParagraph(doc, documentInfo.getDownloadUrl(), 8.0f, urlFont);

		Boolean addedHeader = false;
		Boolean conteDataFirma = false;
		Integer signerNumber = 1;
		for (DocumentSigner sig : documentInfo.getSigners()) {
			if (!addedHeader) {
				setDocumnentParagraph(doc, signerInfoHeader, 9.0f, defaultBoldFont);
				addedHeader = true;
			}

			setDocumnentParagraph(doc, signerLabel, 9.0f, defaultBoldFont);

			// Certificat d'empleat públic
			if (sig.getSignerCN() != null)
				setDocumnentParagraph(doc, sig.getSignerCN(), 9.0f, defaultFont);

			// Informació del representat
			if (sig.isRepresentat()) {
				String inforRepresentat = String.format(signerRepresentant, sig.getRepresentatRaoSocial(), sig.getRepresentatNifCif());
				setDocumnentParagraph(doc, inforRepresentat, 9.0f, defaultFont);
			}
			
			// Certificats amb segell
			if (sig.getSistemaComponent() != null)
				setDocumnentParagraph(doc, sig.getSistemaComponent(), 9.0f, defaultFont);

			if (sig.getPuesto() != null)
				setDocumnentParagraph(doc, sig.getPuesto(), 9.0f, defaultFont);

			if (sig.getSignerOU() != null)
				setDocumnentParagraph(doc, sig.getSignerOU(), 9.0f, defaultFont);

			if (sig.getSignerO() != null)
				setDocumnentParagraph(doc, sig.getSignerO(), 9.0f, defaultFont);

			Paragraph pSigner = new Paragraph();
			doc.add(pSigner);

			if (sig.getDataSignatura() != null) {
				setDocumnentParagraph(doc, signatureDateLabel + ": " + sig.getDataSignatura(), 9.0f, defaultFont);
				conteDataFirma = true;
			}

			if (conteDataFirma) {
				setDocumnentParagraph(doc, advertenciaSignatura, 9.0f, defaultBoldFont);
			}

            if (sig.getReason() != null)
                setDocumnentParagraph(doc, reasonLabel + ": " + sig.getReason(), 9.0f, defaultFont);
			
			if (signerNumber < documentInfo.getSigners().size()) {
				// Separador entre sigants
				setDocumnentParagraph(doc, "***", 9.0f, defaultFont);
			}

			signerNumber++;
		}

		if (documentInfo.getDarrerSegell() != null)
			setDocumnentParagraph(doc, segellDateLabel + ": " + documentInfo.getDarrerSegell(), 9.0f, defaultBoldFont);

		Boolean isNewDigitalArchive = (documentInfo.getDocumentLocation().equals(DocumentLocation.NewDigitalArchive)
				? true
				: false);
		if (isNewDigitalArchive)
			setDocumnentParagraph(doc, metadataENIHeader, 9.0f, defaultBoldFont);
		else
			setDocumnentParagraph(doc, metadataHeader, 9.0f, defaultBoldFont);

		if (documentInfo.getEniDocumentId() != null)
			setDocumnentParagraph(doc, identifierLabel + ": " + documentInfo.getEniDocumentId(), 9.0f, defaultFont);

		setDocumnentParagraph(doc, docNameLabel + ": " + documentInfo.getDocumentName(), 9.0f, defaultFont);

		if (isNewDigitalArchive) {
			String ntiVersion = (documentInfo.getEniNtiVersion() != null ? documentInfo.getEniNtiVersion() : "-");
			setDocumnentParagraph(doc, ntiVersionLabel + ": " + ntiVersion, 9.0f, defaultFont);

			EniDocumentType edt = documentInfo.getEniDocumentType();
			String edtStr = "-";
			if (edt != null)
				edtStr = edt.getDescription(lang);
			setDocumnentParagraph(doc, docTypeLabel + ": " + edtStr, 9.0f, defaultFont);

			EniElaborationStatus es = documentInfo.getEniElaborationStatus();
			String esStr = "-";
			if (es != null)
				esStr = es.getDescription(lang);
			setDocumnentParagraph(doc, elaborationStatusLabel + ": " + esStr, 9.0f, defaultFont);

			String organ = (documentInfo.getEniOrgan() != null ? documentInfo.getEniOrgan() : "-");
			setDocumnentParagraph(doc, organismLabel + ": " + organ, 9.0f, defaultFont);

			String startDateStr = (documentInfo.getDatacaptura() != null ? documentInfo.getDatacaptura() : "-");
			setDocumnentParagraph(doc, startDateLabel + ": " + startDateStr, 9.0f, defaultFont);

			String origin = documentInfo.getEniOrigin();
			String originStr = "-";
			if (origin != null) {
				if ("1".equals(origin)) {
					originStr = adminLabel;
				} else {
					originStr = citizenLabel;
				}
			}
			setDocumnentParagraph(doc, originLabel + ": " + originStr, 9.0f, defaultFont);

			EniSignatureType st = documentInfo.getEniSignType();
			String stStr = "-";
			if (st != null)
				stStr = st.getDescription();
			setDocumnentParagraph(doc, signatureTypeLabel + ": " + stStr, 9.0f, defaultFont);
			
			for (Entry<String, String> e : getOptionalMetadata(lang, documentInfo)) {
				setDocumnentParagraph(doc, e.getKey() + ": " + e.getValue(), 9.0f, defaultFont);
			}

		} else { // Custodia antiga
			String startDateStr = (documentInfo.getDatacaptura() != null ? documentInfo.getDatacaptura() : "-");
			setDocumnentParagraph(doc, startDateLabel + ": " + startDateStr, 9.0f, defaultFont);
			setDocumnentParagraph(doc, noteFootLabel, 9.0f, defaultFont);
		}
		
		setDocumnentParagraph(doc, pagesLabel + ": " + (totalPages + doc.getPdfDocument().getNumberOfPages()), 9.0f, defaultFont);
				
		return page;
	}

	private void setDocumnentParagraph(Document doc, String text, float fontSize, PdfFont font) throws IOException {
		Paragraph p = new Paragraph(text)
				.setFontSize(fontSize)
				.setFont(font);
		p.setFixedLeading(paragraphLeading);
		p.setFirstLineIndent(leftIdentation);
		doc.add(p);
	}

	private void setFooterPage(
			Document document, 
			PdfPage pdfPage,
			DocumentInfo documentInfo, 
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
		BarcodeQRCode qrCode = new BarcodeQRCode(documentInfo.getDownloadUrl());
		PdfFormXObject qrCodeObject = qrCode.createFormXObject(ColorConstants.BLACK, document.getPdfDocument());
		Image qrCodeImage = new Image(qrCodeObject).setWidth(40).setHeight(40);
		float xQR = pageSize.getLeft() + 30;
		float yQR = pageSize.getBottom() + 26;
		qrCodeImage.setFixedPosition(xQR, yQR);

		PdfCanvas pdfCanvas = new PdfCanvas(pdfPage);
		Canvas canvas = new Canvas(pdfCanvas,  pageSize);
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
		p = new Paragraph (documentInfo.getDownloadUrl())
				.setFontSize(fontSize)
				.setFixedPosition(actualPage, xMeta + 36, yMeta + 39, 600);
		canvas.add(p);
		// CSV
		p = new Paragraph (csvLabel + ": " + documentInfo.getHash())
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

	private void copyPdfAttachments(PdfDocument srcPdf, PdfDocument destPdf) throws IOException {
			
		
		PdfDictionary rootDest = destPdf.getCatalog().getPdfObject();
		PdfDictionary namesDest = rootDest.getAsDictionary(PdfName.Names);
		if (namesDest == null) {
			namesDest = new PdfDictionary();
			rootDest.put(PdfName.Names, namesDest);
		}
		PdfDictionary embeddedFilesDest = namesDest.getAsDictionary(PdfName.EmbeddedFiles);
		if (embeddedFilesDest == null) {
			embeddedFilesDest = new PdfDictionary();
			namesDest.put(PdfName.EmbeddedFiles, embeddedFilesDest);
		}
		PdfArray filesArrayDest = embeddedFilesDest.getAsArray(PdfName.Names);
		if (filesArrayDest == null) {
			filesArrayDest = new PdfArray();
			embeddedFilesDest.put(PdfName.Names, filesArrayDest);
		}
		
		PdfDictionary root = srcPdf.getCatalog().getPdfObject();		
		if (root != null) {
			PdfDictionary documentnames = root.getAsDictionary(PdfName.Names);
			if (documentnames != null) {
				PdfDictionary embeddedfiles = documentnames.getAsDictionary(PdfName.EmbeddedFiles);
				if (embeddedfiles != null ) {
					PdfArray filespecs = embeddedfiles.getAsArray(PdfName.Names);
					if (filespecs != null ) {
						for (int i = 0; i < filespecs.size(); i += 2) {
							PdfDictionary fileSpec = filespecs.getAsDictionary(i + 1);
							PdfDictionary ef = fileSpec.getAsDictionary(PdfName.EF);
							if (ef != null) {
								PdfStream fileStream = ef.getAsStream(PdfName.F);
								if (fileStream != null) {
									String embeddedFileName = fileSpec.get(PdfName.F).toString();
							        PdfFileSpec spec = PdfFileSpec.createEmbeddedFileSpec(
							        		destPdf, 
							        		fileStream.getBytes(),
							                null, 
							                embeddedFileName, 
							                null, 
							                embeddedFilesDest, 
							                null);
							        destPdf.addFileAttachment(embeddedFileName, spec);
								}
							}
						}						
					}					
				}
			}
		}
	}

	private void throwAndLogHashException(
			String hash,
			Throwable ex) throws DuplicatedHashException, DocumentNotExistException, GenericServiceException {
		if (ex instanceof DuplicatedHashException) {
			throw (DuplicatedHashException)ex;
		} else if (ex instanceof DocumentNotExistException) {
			throw (DocumentNotExistException)ex;
		} else if (ex instanceof GenericServiceException) {
			log.error("Error al consultar document amb hash: " + hash, ex.getCause());
			throw (GenericServiceException)ex;
		} else {
			log.error("Error al consultar document", ex);
			throw new GenericServiceException("Excepció en el servei del nou arxiu digital", ex);
		}
	}

	/** Consulta la llista de CSV's exclosos. */
	@PermitAll
	@Override
	public List<String> getCsvExclosos() {
		return csvExclosos;
	}

	/** Mètodes per fer tests */

	@Override
	public void setOldSaveKeepingService(OldSaveKeepingServiceInterface oldSaveKeepingService) {
		this.oldSaveKeepingService = oldSaveKeepingService;
	}

	@Override
	public void setNewDigitalArchiveService(NewDigitalArchiveServiceInterface newDigitalArchiveService) {
		this.newDigitalArchiveService = newDigitalArchiveService;
	}

	public void setSubsistemesHelper(SubsistemesHelper subsistemesHelper) {
		this.subsistemesHelper = subsistemesHelper;
	}

	public void setIntegracionsHelper(IntegracionsHelper integracionsHelper) {
		this.integracionsHelper = integracionsHelper;
	}

	public String getLogoPath() {
		return logoPath;
	}

	public void setLogoPath(String logoPath) {
		this.logoPath = logoPath;
	}
	
	public String getExclusionsPath() {
		return exclusionsPath;
	}

	public void setExclusionsPath(String exclusionsPath) {
		this.exclusionsPath = exclusionsPath;
	}

	public void setCsvExclosos(List<String> csvExclosos) {
		this.csvExclosos = csvExclosos;
	}

	/** Classe privada per detectar els canvis de pàgina durant l'edició de la pàgina resum
	 * per tenir el total de pàgines noves correcte. Un cop finalitzada l'edició de la pàgina
	 * resum permet consultar el total d'events de canvi de pàgines produïts.
	 * 
	 */
	private static class CanviPaginaEventHandler implements IEventHandler {
		
		private int canvisPagina = 0;

		public int getCanvisPagina() {
			return canvisPagina;
		}

		@Override
		public void handleEvent(Event currentEvent) {
			PdfDocumentEvent documentEvent = (PdfDocumentEvent) currentEvent;
			canvisPagina++;
		}
	}
}