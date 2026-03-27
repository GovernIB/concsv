package es.caib.concsv.ejb.services;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.annotation.security.PermitAll;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.xml.namespace.QName;

import com.itextpdf.kernel.exceptions.BadPasswordException;
import es.caib.comanda.model.v1.salut.IntegracioApp;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.tika.Tika;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Node;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import es.caib.concsv.ejb.annotation.ErrorInt;
import es.caib.concsv.ejb.annotation.PerformanceInt;
import es.caib.concsv.ejb.utils.PdfSignerUtils;
import es.caib.concsv.ejb.utils.StrUtils;
import es.caib.concsv.ejb.utils.TinyUrlUtils;
import es.caib.concsv.ejb.utils.ValidacioFirmaUtils;
import es.caib.concsv.service.enums.DocumentLocation;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.facade.OldSaveKeepingServiceInterface;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;
import es.caib.concsv.service.model.DocumentSigner;

@ErrorInt
@PerformanceInt
@Stateless @Local(OldSaveKeepingServiceInterface.class)
public class OldSaveKeepingService implements OldSaveKeepingServiceInterface {

    @Inject @ConfigProperty(name = "es.caib.concsv.old.savekeeping.endpoint")
    private String endpoint;
    @Inject @ConfigProperty(name = "es.caib.concsv.query.url")
    private String queryUrl;
    @Inject @ConfigProperty(name = "es.caib.concsv.do.tiny")
    private String reduce;
    @Inject @ConfigProperty(name = "es.caib.concsv.old.savekeeping.timeout", defaultValue = "")
    private String timeout;

    private Logger log = Logger.getLogger(this.getClass());
    private SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss 'GMT'Z");

	@Inject private IntegracionsHelper integracionsHelper;

    @PermitAll
    public DocumentInfo checkHash(String hash) throws GenericServiceException, DuplicatedHashException {
		long t0 = System.currentTimeMillis();
		boolean hasError = true;
        try {
        	hash = hash.trim();
            // TODO: Es poden subtituir les crides amb Axis 1.4?
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress( new java.net.URL(this.endpoint) );
            call.setOperationName(new QName(this.endpoint, "consultarCustodiaXMLPorHash"));
            // Timeout de 60s
            Integer timeout = getTimeoutPropertyValue();
            if (timeout != null) {
                call.setTimeout(timeout);
            }
            byte[] ret = (byte[]) call.invoke( new Object[] { hash } );


            Document document = DocumentHelper.parseText(new String(ret));
            //log.debug(new String(ret));
            DocumentInfo documentInfo = new DocumentInfo();
            Node correctSafeKeepingNode = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:CustodiaOK");
            documentInfo.setCorrectSafeKeeping(getBooleanFromNode(correctSafeKeepingNode));
            Node validHierarchyNode = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:JerarquiaValida");
            documentInfo.setValidHierarchy(getBooleanFromNode(validHierarchyNode));
            Node verifiedHierarchyNode = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:JerarquiaVerificada");
            documentInfo.setVerifiedHierarchy(getBooleanFromNode(verifiedHierarchyNode));
            Node externalCodeNode = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:Documento/vcd:CodigoExterno");
            documentInfo.setDocumentCode(getStringFromNode(externalCodeNode));
            Node documentNameNode = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:Documento/vcd:Nombre");
            String fileName = StrUtils.normalizeString(getStringFromNode(documentNameNode));
            if (!fileName.endsWith(".pdf")) fileName += ".pdf"; // problemes amb iexplorer antics si no tenen extensió
            documentInfo.setDocumentName(fileName);
            Node documentClassNode = document.selectSingleNode("/vcd:ViewCustodiaData/vcd:Documento/vcd:Clase");
            documentInfo.setDocumentType(getStringFromNode(documentClassNode));
            //List<Node> signerNodeList = document.selectNodes("/vcd:ViewCustodiaData/vcd:Sellos/vcd:Sello");
            
            List<DocumentSigner> documentSignerList = new ArrayList<>();
            List<Node> signerNodeList = document.selectNodes("/vcd:ViewCustodiaData/vcd:Firmantes/vcd:Firmante");
            for (Node signerNode : signerNodeList) {
                DocumentSigner documentSigner = new DocumentSigner();
                Node signerCNNode = signerNode.selectSingleNode("vcd:FirmanteCN");
                String strSignerName = getStringFromNode(signerCNNode);
                int index = strSignerName.indexOf(" - ");
                strSignerName = strSignerName.substring(0, (index == -1)? strSignerName.length(): index); // A partir del guió hi ha el dni que no ha de sortir. A l'espera d'una integració amb @firma
                documentSigner.setSignerCN(strSignerName);
                
                Node signerOUNode = signerNode.selectSingleNode("vcd:FirmanteOU");
                documentSigner.setTipoCertificat(getStringFromNode(signerOUNode));
                Node signerOU2Node = signerNode.selectSingleNode("vcd:FirmanteOU2");
                documentSigner.setSignerOU(getStringFromNode(signerOU2Node));
                Node signerTNode = signerNode.selectSingleNode("vcd:FirmanteT");
                documentSigner.setSignerT(getStringFromNode(signerTNode));
                Node signerONode = signerNode.selectSingleNode("vcd:FirmanteO");
                documentSigner.setSignerO(getStringFromNode(signerONode));
                Node signerCIFNode = signerNode.selectSingleNode("vcd:FirmanteCIF");
                documentSigner.setSignerCIF(getStringFromNode(signerCIFNode));
                //documentSigner.setDataSignatura(dataSignatura);
                //documentSigner.setDataSegell(dataSegell);
                documentSignerList.add(documentSigner);
            }
            documentInfo.setSigners(documentSignerList);
            
            List<Node> sellosNodeList = document.selectNodes("/vcd:ViewCustodiaData/vcd:Sellos/vcd:Sello");
            for (Node selloNode : sellosNodeList) {
            	
                Node tipoSelloNode = selloNode.selectSingleNode("vcd:TipoSello");
                String strTipoSello = getStringFromNode(tipoSelloNode);
                Node fechaSelloNode = selloNode.selectSingleNode("vcd:FechaSello");
                String strFechaSello = getStringFromNode(fechaSelloNode);
                // Format: <vcd:FechaSello>31/03/2020 13:48</vcd:FechaSello>
                SimpleDateFormat sdfParse = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                //sdf.setTimeZone(TimeZone.getTimeZone("UTC")); //Esta comentat pq ja es una hora local
                if ("FIRMANTE".equals(strTipoSello)) {                	
                	documentInfo.setDarrerSegell(sdf.format(sdfParse.parse(strFechaSello)));
                }else if ("CUSTODIA".equals(strTipoSello)){
                	documentInfo.setDatacaptura(sdf.format(sdfParse.parse(strFechaSello)));
                }
            }            
            
            documentInfo.setHash(hash);
            documentInfo.setDocumentLocation(DocumentLocation.OldSaveKeeping);
            
            // A custodia nomes hi ha pdf's
            documentInfo.setExtensionFormato("pdf");
            
            String downloadUrl = queryUrl + documentInfo.getHash();
            // Hacer la URL Tiny
            Boolean doTiny = "S".equals(reduce);
            if (doTiny) 
            	documentInfo.setDownloadUrl(TinyUrlUtils.doTinyUrl(downloadUrl, integracionsHelper));
            else 
            	documentInfo.setDownloadUrl(downloadUrl);
            
            byte[] pdfSource = getDocument(hash).getContent();            
            try {
                ValidacioFirmaUtils validacio = new ValidacioFirmaUtils("hash:" + hash, integracionsHelper);
                validacio.setDocument(pdfSource);
                documentInfo.setSigners(validacio.validaFirma());
            } catch (Exception ex) {
            	log.error("Error en la validació de firmes del document " + hash, ex);
            	DocumentSigner docSigner = new DocumentSigner();
            	docSigner.setSignerCN("Certificate Error");
            	documentInfo.getSigners().add(docSigner);
            }
            // Parsetjam el pdf per obtenir la data de signatura
            if ("pdf".equalsIgnoreCase(documentInfo.getExtensionFormato())) {
 				// Per obtenir la data de firma del pdf, en el cas PADES
 				try {
					PdfSignerUtils pdfUtil = new PdfSignerUtils(pdfSource);
					ArrayList<DocumentSigner> padesSignners = pdfUtil.getPdfSigners(documentInfo);
					HashSet<String> hs = new HashSet<String>(); // Si es el mateix segell no el tornam a posar
				    for (DocumentSigner ds : documentInfo.getSigners())
						for (DocumentSigner ds2 : padesSignners) {
							if (ds.getSignerCN()!= null && ds2.getSignerCN()!= null && ds.getSignerCN().startsWith(ds2.getSignerCN()) ||
								ds.getSignerCN()!= null && ds2.getSignerCN()!= null && ds2.getSignerCN().startsWith(ds.getSignerCN()) ||
								ds.getIdEuropeu() != null && ds2.getIdEuropeu()!= null && ds.getIdEuropeu().equals(ds2.getIdEuropeu())
									&& !hs.contains(ds2.getDataSignatura()))
							//&& ds.getDataSignatura() == null
							{
								ds.setDataSegell(ds2.getDataSegell());
								ds.setDataSignatura(ds2.getDataSignatura());
                                ds.setReason(ds2.getReason());
								hs.add(ds2.getDataSignatura());  //Recordam si ja ha estat emprada
							}
					}
				} catch (BadPasswordException e) {
					log.debug("\tDocument amb constrasenya: " + hash + ".");
					documentInfo.setHasPassword(true);
				}
 	            Long darrerSegell = 0L;
 	            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss 'GMT'Z");
 	            for (DocumentSigner d : documentInfo.getSigners()) {
 	            	if (d.getDataSegell() != null)
 	            		darrerSegell = Math.max(darrerSegell, sdf.parse(d.getDataSegell()).getTime());
 	            }
 	            if (darrerSegell != 0) documentInfo.setDarrerSegell(sdf.format(new Date(darrerSegell)));
 	            // Ordenam els firmants per data de la signatura descendent i nom en cas que no tinguin data
 		        Collections.sort(documentInfo.getSigners(), new Comparator<DocumentSigner>() {
 			        @Override
 			        public int compare(DocumentSigner o1, DocumentSigner o2) {
 				        int ret = 0;
 				        // Ordena per data descendent, les nules darreres i finalment per nom.
 				        if (o1 != null && o1.getDataSignatura() != null && o2 != null && o2.getDataSignatura() != null) {
 					        try {
 						        Date dataSignatura1 = sdf.parse(o1.getDataSignatura());
 						        Date dataSignatura2 = sdf.parse(o2.getDataSignatura());
 						        ret = - dataSignatura1.compareTo(dataSignatura2);
 					        } catch(Exception e) {
 						        log.error("Error comparant dates de singatures " + o1.getDataSignatura() + " i " + o2.getDataSignatura());
 						        ret = o1.getSignerCN().compareTo(o2.getSignerCN());
 					        }
 				        } else if (o1 != null && o1.getDataSignatura() != null) {
 					        ret = -1;
 				        } else if (o2 != null && o2.getDataSignatura() != null) {
 					        ret = 1;
 				        } else {
 					        // En cas de que cap tingui data ordena per nom
 					        ret = StringUtils.compare(
 							        o1 != null ? o1.getSignerCN() : null,
 							        o2 != null ? o2.getSignerCN() : null);
 				        }
 				        return ret;
 			        }
 		        });            
 		    }
			hasError = false;
	        return documentInfo;
        } catch (Exception ex) {
        	String message = ex.getMessage();
            if (message == null || !message.contains("es.caib.signatura.error.ReservaNotFoundException")) {
            	log.error("Error consultant el servei antic: " + message, ex);
                throw new GenericServiceException(ex);
            }
        } finally {
			integracionsHelper.addOperation(IntegracioApp.SIG, t0, hasError);
		}
        return null;
    }

    /** Llegeix la propietat pel timeout. És un text amb el valor en ms.
     * 
     * @return
     */
    private Integer getTimeoutPropertyValue() {
    	Integer timeoutValue = null;
    	if (timeout != null && !timeout.isEmpty()) {
    		try {
    			timeoutValue = Integer.valueOf(timeout);
    		} catch(Exception e) {
    			log.error("Error convertint a enter el valor pel timeout del servei antic de custodia: " + timeout);
    		}
    	}
		return timeoutValue;
	}

	@PermitAll
    public DocumentContent getDocument(String hash) throws GenericServiceException {
		long t0 = System.currentTimeMillis();
		boolean hasError = true;
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(this.endpoint) );
            call.setOperationName(new QName(this.endpoint, "consultarDocumentoPorHash"));
            byte[] ret = (byte[]) call.invoke( new Object[] { hash } );
            if (ret != null) {
                DocumentContent dc = new DocumentContent();                
                dc.setCsv(hash);
                dc.setContent(ret);
                Tika tika = new Tika();
                String mime = tika.detect(ret);
                dc.setMimeType(mime);
				hasError = false;
                return dc;
            }
        } catch (Exception e) {
            log.error(e);
        } finally {
			integracionsHelper.addOperation(IntegracioApp.SIG, t0, hasError);
		}
        return null;
    }

    private Boolean getBooleanFromNode(Node nodeValue) {
        if (nodeValue == null) {
            return false;
        } else {
            String value = nodeValue.getText();
            return value.equals("S")?true:(value.equals("N")?false: false);
        }
    }

    private String getStringFromNode(Node nodeValue) {
        if (nodeValue == null) {
            return "";
        } else {
            return nodeValue.getText();
        }
    }



}
