package es.caib.concsv.service.facade;

import es.caib.concsv.service.exception.DuplicatedHashException;
import es.caib.concsv.service.exception.GenericServiceException;
import es.caib.concsv.service.model.DocumentContent;
import es.caib.concsv.service.model.DocumentInfo;

public interface OldSaveKeepingServiceInterface {
    DocumentInfo checkHash(String hash) throws GenericServiceException, DuplicatedHashException;
    DocumentContent getDocument(String hash) throws GenericServiceException;
}
