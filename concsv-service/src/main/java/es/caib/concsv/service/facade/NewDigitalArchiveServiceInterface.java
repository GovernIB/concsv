package es.caib.concsv.service.facade;

import es.caib.concsv.service.exception.DocumentNotExistException;
import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;

public interface NewDigitalArchiveServiceInterface {
    DocumentInfo checkHash(String hash) throws DuplicatedHashException, DocumentNotExistException, GenericServiceException;
    DocumentInfo checkHashFromUUID(String uuid, String hash) throws DocumentNotExistException, GenericServiceException;
    DocumentContent getDocument(String uuid, Boolean packedFile) throws GenericServiceException;
    DocumentContent getEniDocument(String uuid) throws GenericServiceException;
}
