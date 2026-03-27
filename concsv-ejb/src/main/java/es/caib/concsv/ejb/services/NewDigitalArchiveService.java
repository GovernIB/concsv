package es.caib.concsv.ejb.services;

import com.itextpdf.kernel.exceptions.BadPasswordException;
import es.caib.arxiudigital.apirest.ApiArchivoDigital;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.DocumentNode;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.Metadata;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.ResParamSearchDocument;
import es.caib.arxiudigital.apirest.CSGD.entidades.comunes.RespuestaGenerica;
import es.caib.arxiudigital.apirest.CSGD.entidades.resultados.SearchDocsResult;
import es.caib.arxiudigital.apirest.facade.pojos.CabeceraPeticion;
import es.caib.arxiudigital.apirest.facade.pojos.Documento;
import es.caib.arxiudigital.apirest.facade.pojos.FirmaDocumento;
import es.caib.arxiudigital.apirest.facade.resultados.Resultado;
import es.caib.comanda.model.v1.salut.IntegracioApp;
import es.caib.concsv.ejb.annotation.ErrorInt;
import es.caib.concsv.ejb.annotation.PerformanceInt;
import es.caib.concsv.ejb.helpers.IntegracionsHelper;
import es.caib.concsv.ejb.utils.*;
import es.caib.concsv.service.enums.DocumentLocation;
import es.caib.concsv.service.enums.EniDocumentType;
import es.caib.concsv.service.enums.EniElaborationStatus;
import es.caib.concsv.service.enums.EniSignatureType;
import es.caib.concsv.service.exception.DocumentNotExistException;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.facade.NewDigitalArchiveServiceInterface;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;
import es.caib.concsv.service.model.DocumentSigner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@ErrorInt
@PerformanceInt
@Stateless @Local(NewDigitalArchiveServiceInterface.class)
public class NewDigitalArchiveService implements NewDigitalArchiveServiceInterface {

    @Inject @ConfigProperty(name = "es.caib.concsv.new.digital.archive.endpoint")
    private String endpoint;
    @Inject @ConfigProperty(name = "es.caib.concsv.query.url")
    private String queryUrl;
    @Inject @ConfigProperty(name = "es.caib.concsv.do.tiny")
    private String reduce;
    @Inject @ConfigProperty(name = "es.caib.concsv.new.digital.archive.organization")
    private String ORGANIZACION;
    @Inject @ConfigProperty(name = "es.caib.concsv.new.digital.archive.app.client")
    private String APLICACION_CLIENTE;
    @Inject @ConfigProperty(name = "es.caib.concsv.new.digital.archive.username")
    private String USERNAME;
    @Inject @ConfigProperty(name = "es.caib.concsv.new.digital.archive.password")
    private String PASSWORD;
    @Inject @ConfigProperty(name = "es.caib.concsv.new.digital.archive.version")
    private String VERSION_SERVICIO;
	@Inject @ConfigProperty(name = "es.caib.concsv.new.digital.archive.traces", defaultValue = "true")
	private boolean TRACES;

	SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss 'GMT'Z");
	List<String> listDetachedDocuments = Arrays.asList("TF04");
	List<String> listAtachedDocuments = Arrays.asList("TF02","TF03","TF05","TF06");

	@Inject private IntegracionsHelper integracionsHelper;

	@PermitAll
	public DocumentInfo checkHash(String hash) throws DuplicatedHashException, DocumentNotExistException, GenericServiceException {
		long t0 = System.currentTimeMillis();
		Boolean hasError = false;
        try {
        	hash = hash.trim();
        	// Si el hash ja no ne el format de l'arxiu evitam fer la petició.
        	if (!hash.matches("^([0-9a-f]{64})$")) {
				log.debug("\tCodi amb format erroni: " + hash + ".");
				hasError = null;
				return null;
			}

            ApiArchivoDigital apiArxiu = getApiArxiu();
            String query = "(+TYPE:\"eni:documento\" AND @eni\\:csv:\""+hash+"\" -ASPECT:\"gdib:borrador\" -ASPECT:\"gdib:trasladado\") " +
                    " OR (+TYPE:\"gdib:documentoMigrado\" AND @gdib\\:hash:\""+hash+"\") ";
            SearchDocsResult result = apiArxiu.busquedaDocumentos(query, 0);
            if (result == null) {
				throw new GenericServiceException("La petició a l’API d’Arxiu Digital no s’ha processat correctament.");
			}
            RespuestaGenerica<ResParamSearchDocument> respuestaGenerica = result.getSearchDocumentsResult();
            String resultCode = respuestaGenerica.getResult().getCode();

            if (resultCode.equals("COD_000")) {
                // Ha ido bien
            } else if (resultCode.equals("COD_001")) {
                throw new DocumentNotExistException("csv:" + hash);
            } else {
                throw new GenericServiceException(respuestaGenerica.getResult().getCode()+"-"+respuestaGenerica.getResult().getDescription());
            }

            ResParamSearchDocument resParam = respuestaGenerica.getResParam();
            DocumentNode documentNode = null;
            if (resParam.getTotalNumberOfResults() > 1) {
                List<DocumentNode> documentNodeList = resParam.getDocuments();
                String documentEniId = "";
                Boolean duplicated = false;
                for (DocumentNode dn : documentNodeList) {
                    String actualEniId = getEniID(dn);
                    if (documentEniId.equals("")) { // Primer documento
                        documentEniId = actualEniId;
                    } else if (!documentEniId.equals(actualEniId)) { // Resto de documentos
                        duplicated = true;
                        break;
                    }
                }
                if (duplicated) {
                    throw new DuplicatedHashException("La consulta del hash " + hash + " ha retornat més d'un document");
                }
            }
            // Si hubiera duplicados habríamos lanzado un excepción de tipo DuplicatedHashException
            // Cómo no existen duplicados se puede recuperar cualquiera de los documentos recuperados
            documentNode = resParam.getDocuments().get(0);
            return checkHashFromUUID(documentNode.getId(), hash); // UUID alfresco
            
        } catch (DuplicatedHashException | GenericServiceException | DocumentNotExistException ex) {
			hasError = true;
            throw ex;
        } catch (Exception ex) {
			hasError = true;
            throw new GenericServiceException(ex);
        } finally {
			integracionsHelper.addOperation(IntegracioApp.ARX, t0, hasError);
		}
    }

    @PermitAll
	//@RolesAllowed({"CSV_REST"})
    public DocumentInfo checkHashFromUUID(String uuid, String hash) throws DocumentNotExistException, GenericServiceException {
    	//uuid = "4c66b473-347a-45d1-bc22-e13625d55cf1";2adeae575ee78ae3e1f8cf20f4eb7fe8b2a65834cbfa194395a65dc23a417aa5
		long t0 = System.currentTimeMillis();
		boolean hasError = false;
        try {
	        ApiArchivoDigital apiArxiu = getApiArxiu();
            Resultado<Documento> results = apiArxiu.obtenerDocumento(uuid, true);
            //DocumentMetadades
            Documento documentoDevuelto = results.getElementoDevuelto();
            if (documentoDevuelto == null) {
	            throw new DocumentNotExistException("uuid:" + uuid);
            }
            DocumentInfo documentInfo = new DocumentInfo();            
            documentInfo.setDocumentLocation(DocumentLocation.NewDigitalArchive);            

            Map<String, Object> metaMap = documentoDevuelto.getMetadataCollection();
            documentInfo.setMetadata(metaMap);            
            
            documentInfo.setEniDocumentId(getGenericMetadata(metaMap, "eni:id"));
            documentInfo.setEniOrgan(getGenericMetadata(metaMap, "eni:organo"));
            String docType = getGenericMetadata(metaMap, "eni:tipo_doc_ENI");
            if (docType != null) documentInfo.setEniDocumentType(EniDocumentType.valueOf(docType));
            String signType = getGenericMetadata(metaMap, "eni:tipoFirma");
            if (signType != null) documentInfo.setEniSignType(EniSignatureType.valueOf(signType));
            String fileName = StrUtils.normalizeString(documentoDevuelto.getName());
            documentInfo.setDocumentName(fileName);                     
            String elbStatus = getGenericMetadata(metaMap, "eni:estado_elaboracion");
            if (elbStatus != null) documentInfo.setEniElaborationStatus(EniElaborationStatus.valueOf(elbStatus));
            documentInfo.setEniOrigin(getGenericMetadata(metaMap, "eni:origen"));
            documentInfo.setEniNtiVersion(getGenericMetadata(metaMap, "eni:v_nti"));
            String dataInici = getGenericMetadata(metaMap, "eni:fecha_inicio");
            if (dataInici != null) {
            	SimpleDateFormat sdfParseArxiu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            	documentInfo.setDatacaptura(sdf.format(sdfParseArxiu.parse(dataInici)));            	
            }
            documentInfo.setDocumentType(documentoDevuelto.getType().getValue());
            documentInfo.setDocumentCode(documentoDevuelto.getId());
            if (documentoDevuelto.getMetadataCollection().containsKey("gdib:hash")) {
                Object hashh = documentoDevuelto.getMetadataCollection().get("gdib:hash");
                if (hashh instanceof Integer) {
                    documentInfo.setHash(String.valueOf(hashh));
                } else if (hashh instanceof String) {
                    documentInfo.setHash((String) hashh);
                } else {
                	documentInfo.setHash(hashh.toString());
                }
            } else {
                documentInfo.setHash((String) documentoDevuelto.getMetadataCollection().get("eni:csv"));
            }
                        
            documentInfo.setExtensionFormato(getGenericMetadata(metaMap, "eni:extension_formato").replace(".", ""));
            
            String downloadUrl = queryUrl + documentInfo.getHash();
            // Hacer la URL Tiny
            Boolean doTiny = "S".equals(reduce);
            if (doTiny) 
            	documentInfo.setDownloadUrl(TinyUrlUtils.doTinyUrl(downloadUrl, integracionsHelper));
            else 
            	documentInfo.setDownloadUrl(downloadUrl);            
            
	        byte[] pdfSource = Base64.getDecoder().decode(documentoDevuelto.getContent().getBytes());

            documentInfo.setSigners(new ArrayList<DocumentSigner>());

            ValidacioFirmaUtils validacioFirma = new ValidacioFirmaUtils(
				hash != null ? "hash:" + hash : "uuid:" + uuid,
				integracionsHelper);
	        validacioFirma.setDocument(pdfSource);
            if (listAtachedDocuments.contains(signType)) {
	            try {
                    List<DocumentSigner> signatures = validacioFirma.validaFirma();
	            	documentInfo.getSigners().addAll(signatures);
	            } catch (Exception ex) {
		            log.error("Error en la validació de firmes de tipus attached del document " + hash, ex);
	            	DocumentSigner docSigner = new DocumentSigner();
	            	docSigner.setSignerCN("Certificate Error");
	            	documentInfo.getSigners().add(docSigner);
	            }
            } else if (listDetachedDocuments.contains(signType)) { // Signatura detached
            	List<FirmaDocumento> firmas = documentoDevuelto.getListaFirmas();
            	for (FirmaDocumento firma : firmas) {
            		byte[] pdfSignature = Base64.getDecoder().decode(firma.getContent().getBytes());
            		try {
            			validacioFirma.setSignatura(pdfSignature);
                        List<DocumentSigner> signatures = validacioFirma.validaFirma();
            			documentInfo.getSigners().addAll(signatures);
            		} catch (Exception ex) {
			            log.error("Error en la validació de firmes de tipus detached del document " + hash, ex);
            			DocumentSigner docSigner = new DocumentSigner();
            			docSigner.setSignerCN("Certificate Error");
    	            	documentInfo.getSigners().add(docSigner);
            		}
            	}
            }
           /* if (documentInfo.getSigners() != null) {
                log.debug("SignatureDetailInfo: " + documentInfo.getSigners().size());
                log.debug("Firmas: " + documentInfo.getSigners().size());
            }*/
            /*if (validacioFirma.getDataSegell() != null) {
            	DocumentStamp documentStamp = new DocumentStamp();
            	documentStamp.setDateStamp(sdf.format(validacioFirma.getDataSegell()));
            	documentInfo.setDocumentStamp(documentStamp);
            }*/

            if ("pdf".equalsIgnoreCase(documentInfo.getExtensionFormato())) {
				// En els documents PDF amb més d'una firma, la data de firma que ens dona el plugin de validació per
				// a totes les firmes és la data del segell de temps.
				// Per a mostrar correctament la data de la firma s'extreuen les dates directament de les firmes
				// document mitjançant iText.
				try {
					PdfSignerUtils pdfUtil = new PdfSignerUtils(pdfSource);
					ArrayList<DocumentSigner> padesSignners = pdfUtil.getPdfSigners(documentInfo);
					HashSet<String> hs = new HashSet<String>();
					for (DocumentSigner ds : documentInfo.getSigners()) {
						for (DocumentSigner ds2 : padesSignners) {
							if (ds.getSignerCN() != null && ds2.getSignerCN() != null && ds.getSignerCN().startsWith(ds2.getSignerCN()) ||
								ds.getSignerCN() != null && ds2.getSignerCN() != null && ds2.getSignerCN().startsWith(ds.getSignerCN()) ||
								ds.getIdEuropeu() != null && ds2.getIdEuropeu() != null && ds.getIdEuropeu().equals(ds2.getIdEuropeu())
									&& !hs.contains(ds2.getDataSignatura())) {
								ds.setDataSignatura(ds2.getDataSignatura());
                                ds.setReason(ds2.getReason());
								hs.add(ds2.getDataSignatura());
							}
						}
					}
				} catch (BadPasswordException e) {
					log.debug("\tDocument amb constrasenya: " + hash + ".");
					documentInfo.setHasPassword(true);
				}
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
				        } catch (Exception ex) {
					        log.error("Error comparant les dates de les firmes " + o1.getDataSignatura() + " i " + o2.getDataSignatura() + " pel document " + hash);
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
            return documentInfo;
        } catch (DocumentNotExistException ex) {
			hasError = true;
			throw ex;
        } catch (Exception ex) {
			hasError = true;
            throw new GenericServiceException(ex);
        } finally {
			integracionsHelper.addOperation(IntegracioApp.ARX, t0, hasError);
		}
    }

    private String getEniID(DocumentNode dn) {
        return getGenericMetadata(dn, "eni:id");
    }

    private String getGenericMetadata(DocumentNode dn, String metadataName) {
        List<Metadata> metadataList = dn.getMetadataCollection();
        for (Metadata metadata : metadataList) {
            if (metadata.getQname().equals(metadataName)) {
                if (metadata.getValue() instanceof Integer) {
                    return ((Integer) metadata.getValue()).toString();
                } else if (metadata.getValue() instanceof Date) {                    
                    return sdf.format((Date) metadata.getValue());
                } else if (metadata.getValue() instanceof List) {
                    List<String> list = (List) metadata.getValue();
                    return StringUtils.join(list.toArray(),", ");
                } else {
                    return (String) metadata.getValue();
                }
            }
        }
        return null;
    }

    private String getGenericMetadata(Map<String, Object> metaMap, String metadataName) {
        Object o = metaMap.get(metadataName);
        if (o != null) {
            if (o instanceof Integer) {
                return ((Integer) o).toString();
            } else if (o instanceof Date) {                
                return sdf.format((Date) o);
            } else {
                return (String) o;
            }
        }
        return null;
    }

	@PermitAll
    public DocumentContent getDocument(String uuid, Boolean packedFile) throws GenericServiceException {
		long t0 = System.currentTimeMillis();
		Boolean hasError = false;
        try {
            if (uuid == null) {
				hasError = null;
				return null;
			}
            String fileSigExtension = null;

	        ApiArchivoDigital apiArxiu = getApiArxiu();
            Resultado<Documento> result = apiArxiu.obtenerDocumento(uuid, true);
            String fileExtension = getGenericMetadata(result.getElementoDevuelto().getMetadataCollection(), "eni:extension_formato");
            
            if (result.getElementoDevuelto() != null && result.getElementoDevuelto().getContent() != null) {
                DocumentContent dc = new DocumentContent();
                String signType = getGenericMetadata(result.getElementoDevuelto().getMetadataCollection(), "eni:tipoFirma");
                
                String fileName = (result.getElementoDevuelto().getName().toLowerCase().endsWith(fileExtension.toLowerCase()))?
    					result.getElementoDevuelto().getName():
    					result.getElementoDevuelto().getName() + fileExtension;
    					
            	if (listDetachedDocuments.contains(signType) && packedFile) {
            		ZipUtils zip = new ZipUtils();
            		try {            			
            			zip.addFile(fileName, Base64.getDecoder().decode(result.getElementoDevuelto().getContent()));
            			List<FirmaDocumento> signersList = result.getElementoDevuelto().getListaFirmas();
            			if ("TF02".equals(signType))
            				fileSigExtension = ".xsig";
            			else if ("TF04".equals(signType))
            				fileSigExtension = ".csig";
            					
        	            for (int i = 0; i < signersList.size(); i++) {
        	                zip.addFile("signer" + i + fileSigExtension, Base64.getDecoder().decode(signersList.get(i).getContent()));
        	            }
        			} catch (IOException e) {
        				log.error("Error generant el zip", e);
        			}
            		dc.setFileName(result.getElementoDevuelto().getName().toLowerCase().replaceAll(fileExtension.toLowerCase() + "$", "") + ".zip");
                    dc.setContent(zip.generate().toByteArray());                    
                    dc.setMimeType("application/zip");
            	} else {
           			dc.setFileName(fileName);
            		dc.setContent(Base64.getDecoder().decode(result.getElementoDevuelto().getContent()));
            	}
                if (result.getElementoDevuelto().getMetadataCollection().containsKey("gdib:hash")) {
                    Object hash = result.getElementoDevuelto().getMetadataCollection().get("gdib:hash");
                    if (hash instanceof Integer) {
                        dc.setCsv(String.valueOf(hash));
                    } else if (hash instanceof Integer) {
                        dc.setCsv((String) hash);
                    }
                } else {
                    dc.setCsv((String) result.getElementoDevuelto().getMetadataCollection().get("eni:csv"));
                }
                Tika tika = new Tika();
                String mime = tika.detect(dc.getContent());
                dc.setMimeType(mime);
                return dc;
            }
        } catch (Exception e) {
			hasError = true;
            throw new GenericServiceException(e);
        } finally {
			integracionsHelper.addOperation(IntegracioApp.ARX, t0, hasError);
		}
        return null;
    }

    @PermitAll
	//@RolesAllowed({"CSV_REST"})
    public DocumentContent getEniDocument(String uuid) throws GenericServiceException {
		long t0 = System.currentTimeMillis();
		Boolean hasError = null;
        try {
            if (uuid == null) return null;
            ApiArchivoDigital apiArxiu = getApiArxiu();
            Resultado<String> result = apiArxiu.obtenerDocumentoENI(uuid);
            if (result.getElementoDevuelto() != null) {
                DocumentContent dc = new DocumentContent();
                dc.setContent(result.getElementoDevuelto().getBytes());
                Tika tika = new Tika();
                String mime = tika.detect(dc.getContent());
                dc.setMimeType(mime);
				hasError = false;
                return dc;
            }
        } catch (Exception e) {
			hasError = true;
            throw new GenericServiceException(e);
        } finally {
			integracionsHelper.addOperation(IntegracioApp.ARX, t0, hasError);
		}
        return null;
    }

    private CabeceraPeticion getCabecera() {
        CabeceraPeticion cabecera = new CabeceraPeticion();
        cabecera.setServiceVersion(VERSION_SERVICIO);
        cabecera.setCodiAplicacion(APLICACION_CLIENTE);
        cabecera.setUsuarioSeguridad(USERNAME);
        cabecera.setPasswordSeguridad(PASSWORD);
        cabecera.setOrganizacion(ORGANIZACION);
        cabecera.setNombreUsuario("$concsv$");
        cabecera.setNombreSolicitante("Anònim");
        cabecera.setDocumentoSolicitante("Anònim");
        cabecera.setNombreProcedimiento("Consulta CSV");
        return cabecera;
    }

    private ApiArchivoDigital getApiArxiu() {
        CabeceraPeticion cabecera = getCabecera();
        ApiArchivoDigital apiArxiu = new ApiArchivoDigital(this.endpoint, cabecera);
        apiArxiu.setTrazas(TRACES);
        return apiArxiu;
    }

}
