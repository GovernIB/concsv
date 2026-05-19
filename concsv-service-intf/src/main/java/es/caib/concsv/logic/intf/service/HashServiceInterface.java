package es.caib.concsv.logic.intf.service;

import es.caib.concsv.logic.intf.exception.DocumentNotExistException;
import es.caib.concsv.logic.intf.exception.DuplicatedHashException;
import es.caib.concsv.logic.intf.exception.GenericServiceException;
import es.caib.concsv.logic.intf.model.DocumentContent;
import es.caib.concsv.logic.intf.model.DocumentInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public interface HashServiceInterface {
    DocumentInfo checkHash(String hash) throws GenericServiceException, DuplicatedHashException, DocumentNotExistException;
    DocumentInfo checkHashFromUUID(String uuid) throws DuplicatedHashException, DocumentNotExistException, GenericServiceException;
    DocumentContent getDocument(DocumentInfo documentInfo, Boolean packedFile) throws GenericServiceException;
    DocumentContent getEniDocument(DocumentInfo documentInfo) throws GenericServiceException;
    DocumentContent getPrintableDocument(DocumentInfo documentInfo, String lang) throws GenericServiceException, DuplicatedHashException, DocumentNotExistException;
    ArrayList<Entry<String, String>> getOptionalMetadata(String lang, DocumentInfo documentInfo);
	/** Consulta la llista de CSV's exclosos. */
	List<String> getCsvExclosos();

    // Per fer tests
    public void setOldSaveKeepingService(OldSaveKeepingServiceInterface oldSaveKeepingService);
    public void setNewDigitalArchiveService(NewDigitalArchiveServiceInterface newDigitalArchiveService);
}
