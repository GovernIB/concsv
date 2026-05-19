package es.caib.concsv.logic.intf.service;

import es.caib.concsv.logic.intf.exception.DuplicatedHashException;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import es.caib.concsv.logic.intf.model.DocumentContent;
import es.caib.concsv.logic.intf.model.DocumentInfo;

public interface OldSaveKeepingServiceInterface {
    DocumentInfo checkHash(String hash) throws GenericServiceException, DuplicatedHashException;
    DocumentContent getDocument(String hash) throws GenericServiceException;
}
