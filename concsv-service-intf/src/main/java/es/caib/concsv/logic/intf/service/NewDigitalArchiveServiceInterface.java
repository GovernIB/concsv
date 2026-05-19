package es.caib.concsv.logic.intf.service;

import es.caib.concsv.logic.intf.exception.DocumentNotExistException;
import es.caib.concsv.logic.intf.exception.DuplicatedHashException;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import es.caib.concsv.logic.intf.model.DocumentContent;
import es.caib.concsv.logic.intf.model.DocumentInfo;

public interface NewDigitalArchiveServiceInterface {
    DocumentInfo checkHash(String hash) throws DuplicatedHashException, DocumentNotExistException, GenericServiceException;
    DocumentInfo checkHashFromUUID(String uuid, String hash) throws DocumentNotExistException, GenericServiceException;
    DocumentContent getDocument(String uuid, Boolean packedFile) throws GenericServiceException;
    DocumentContent getEniDocument(String uuid) throws GenericServiceException;
}
