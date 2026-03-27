package es.caib.concsv.ejb.service.test.service;

import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import es.caib.concsv.ejb.helpers.SubsistemesHelper;
import es.caib.concsv.ejb.services.HashService;
import es.caib.concsv.ejb.utils.MyDocumentConverter;
import es.caib.concsv.service.enums.DocumentLocation;
import es.caib.concsv.service.enums.EniDocumentType;
import es.caib.concsv.service.enums.EniElaborationStatus;
import es.caib.concsv.service.enums.EniSignatureType;
import es.caib.concsv.service.facade.HashServiceInterface;
import es.caib.concsv.service.facade.NewDigitalArchiveServiceInterface;
import es.caib.concsv.service.facade.OldSaveKeepingServiceInterface;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;
import es.caib.concsv.service.model.DocumentSigner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HashServiceTest {

	private static HashServiceInterface hashService;
	private static DocumentInfo docInfo;
	private static DocumentContent dc;

	private static String[] arxius = new String[] {
			
			"b4e15fbae8e26dadba7e2cfd480e33ea89e83bf3cf737ea5d8223bfa8b627178", // exemple real
			"DIN1-1_signada_comentat", // formulari i comentari
			"28_error_obrint", // error obrint #28
			"annex_sense_firma", // amb un comentari que té un import i amb problemes de rotació
			"planols_comentat" // plànols A2 amb rotació 270
	};

	//private static String arxiu = "b4e15fbae8e26dadba7e2cfd480e33ea89e83bf3cf737ea5d8223bfa8b627178"; // exemple real
	//private static String arxiu = "DIN1-1_signada_comentat"; // formulari i comentari
	//private static String arxiu = "28_error_obrint"; // error obrint #28
	//private static String arxiu = "annex_sense_firma"; // amb un comentari que té un import i amb problemes de rotació
	//private static String arxiu = "planols_comentat"; // plànols A2 amb rotació 270
	//private static String arxiu = "imatge"; // prova d'imatge amb extensió jpg
	//private static String arxiu = "pdf amb adjunt"; // document amb fitxers de text com adjunts, la versió imprimible també els ha de tenir
	//private static String arxiu = "pagines en blanc"; // #44 la versió imprimible té totes les págines en blanc
	//private static String arxiu = "blank_protected"; // error obrint pdf protegit amb password.
	//private static String arxiu = "stackoverflow"; // error obrint pdf protegit amb password.
	private static String arxiu = "invalid_reference"; // formulari i comentari
	private static String extensio = "pdf"; //pdf/jpg
	
	
	@BeforeClass
	public static void setUp() throws Exception {
		String logoPath = "/opt/webapps/concsv/logoGOIBc.png";
		String exclusionsPath = "/opt/webapps/concsv/documents-exclosos.txt";
		
		System.setProperty("es.caib.concsv.consult.oldSafeKeeping", "N");
		System.setProperty("es.caib.concsv.consult.newDigitalArchive", "S");
		System.setProperty("es.caib.concsv.logo.path", logoPath);
		System.setProperty("es.caib.concsv.convertpdf2img", "false");
		//System.setProperty("es.caib.concsv.arxiu.documents.exclosos.path", exclusionsPath);

		// Helpers
		SubsistemesHelper subsistemesHelper = new SubsistemesHelper();
		IntegracionsHelper integracionsHelper = new IntegracionsHelper();
		
		docInfo = new DocumentInfo();
		docInfo.setExtensionFormato(extensio);
		docInfo.setDocumentLocation(DocumentLocation.NewDigitalArchive);
		docInfo.setDocumentName(arxiu + "." + extensio);
		docInfo.setDocumentCode("00000000");
		docInfo.setHash(arxiu);
		docInfo.setDownloadUrl("https://dev.caib.es/concsvfront/view.xhtml?hash=" + docInfo.getHash());
		List<DocumentSigner> signers = new ArrayList<DocumentSigner>();
		DocumentSigner signer = new DocumentSigner();
		signer.setSignerCN("SIG_CN_1");
		signer.setSistemaComponent("SIG_COMP_1");
		signer.setPuesto("SIG_PIESTO_1");
		signer.setSignerOU("SIG_OU_1");
		signer.setSignerO("SIG_O_1");
		signer.setDataSignatura("01/01/2020");
		signers.add(signer);
		signer = new DocumentSigner();
		signer.setSignerCN("SIG_CN_2");
		signer.setSistemaComponent("SIG_COMP_2");
		signer.setPuesto("SIG_PIESTO_2");
		signer.setSignerOU("SIG_OU_2");
		signer.setSignerO("SIG_O_2");
		signer.setDataSignatura("02/01/2020");
		signers.add(signer);
		signer = new DocumentSigner();
		signer.setSignerCN("SIG_CN_3");
		signer.setSistemaComponent("SIG_COMP_3");
		signer.setPuesto("SIG_PIESTO_3");
		signer.setSignerOU("SIG_OU_3");
		signer.setSignerO("SIG_O_3");
		signer.setDataSignatura("03/01/2020");
		signers.add(signer);
		docInfo.setSigners(signers);
		docInfo.setDarrerSegell("01/01/2020");
		docInfo.setEniDocumentId("ENI_ID");
		docInfo.setEniNtiVersion("NTI_VERSION");
		docInfo.setEniDocumentType(EniDocumentType.TD99);
		docInfo.setEniElaborationStatus(EniElaborationStatus.EE99);
		docInfo.setEniOrgan("A04003003");
		docInfo.setDatacaptura("01/01/2020");
		docInfo.setEniOrigin("2");
		docInfo.setEniSignType(EniSignatureType.TF01);
		
		// Metadades addicionals
		docInfo.setMetadata(new HashMap<String, Object>());
		docInfo.getMetadata().put("Metadada1", "Valor metadada 1");
		docInfo.getMetadata().put("Metadada2", Integer.valueOf(2));
		docInfo.getMetadata().put("Metadada3", Float.valueOf(2));
		
		Path path = Paths.get("/tmp/concsv/" + arxiu + "." + extensio);
		byte[] contingut = Files.readAllBytes(path);
		dc = new DocumentContent();
		dc.setContent(contingut);
		if ("pdf".equals(extensio)) {
			dc.setMimeType("application/pdf");
		} else if ("jpg".equals(extensio)) {
			dc.setMimeType("image/jpeg");
		} else {
			MyDocumentConverter docConv = new MyDocumentConverter(integracionsHelper);
			dc.setMimeType(docConv.getMimeByExtension(extensio));
		}
		
		HashService hashService = new HashService();		
		hashService.setOldSaveKeepingService(configureMockOldSaveKeepingService());
		hashService.setNewDigitalArchiveService(configureMockNewDigitalArchiveService());
		hashService.setLogoPath(logoPath);
		hashService.setExclusionsPath(exclusionsPath);
		hashService.setSubsistemesHelper(subsistemesHelper);
		hashService.setIntegracionsHelper(integracionsHelper);
		hashService.init();
		
		HashServiceTest.hashService = hashService;
	}
	
	@Test
	public void testCheckHashPdfForm() throws Exception{
		
		DocumentContent dc = hashService.getPrintableDocument(docInfo, "ca");
		Path path = Paths.get("/tmp/concsv/itext8/" + arxiu +"_imprimible.pdf");
		Files.write(path, dc.getContent());
		System.out.println("Document convertit i guardat a: " + path);	
	}

	private static OldSaveKeepingServiceInterface configureMockOldSaveKeepingService() throws Exception {
		OldSaveKeepingServiceInterface oldSaveKeepingServiceMock = Mockito.mock(OldSaveKeepingServiceInterface.class);
		
		Mockito.when(oldSaveKeepingServiceMock.checkHash(Mockito.anyString())).thenReturn(docInfo);
		Mockito.when(oldSaveKeepingServiceMock.getDocument(Mockito.anyString())).thenReturn(dc);
		return oldSaveKeepingServiceMock;
	}
	
	private static NewDigitalArchiveServiceInterface configureMockNewDigitalArchiveService() throws Exception {
		NewDigitalArchiveServiceInterface newDigitalArchiveServiceMock = Mockito.mock(NewDigitalArchiveServiceInterface.class);

		Mockito.when(newDigitalArchiveServiceMock.checkHash(Mockito.anyString())).thenReturn(docInfo);
		Mockito.when(newDigitalArchiveServiceMock.getDocument(Mockito.anyString(), Mockito.anyBoolean())).thenReturn(dc);
		return newDigitalArchiveServiceMock;
	}
	
}
