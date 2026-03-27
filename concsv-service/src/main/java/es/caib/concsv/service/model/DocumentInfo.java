package es.caib.concsv.service.model;

import es.caib.concsv.service.enums.DocumentLocation;
import es.caib.concsv.service.enums.EniDocumentType;
import es.caib.concsv.service.enums.EniElaborationStatus;
import es.caib.concsv.service.enums.EniSignatureType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter
public class DocumentInfo {

    private DocumentLocation documentLocation;
    @Setter(AccessLevel.NONE)
    private String eniDoc;
    private Boolean correctSafeKeeping;
    private Boolean validHierarchy;
    private Boolean verifiedHierarchy;
    private String documentCode;
    private String documentName;
    private String documentType;
    private EniDocumentType eniDocumentType;
    private String extensionFormato;
	private String hash;
    private Boolean printable;    
    private Boolean csvExclos;
    private Boolean amagarBotoOriginal;
	private Boolean hasPassword;
    private Boolean malformatted;
    private String eniDocumentId;
    private String eniOrgan;
    
    private String eniOrigin;
    private String eniNtiVersion;
    private EniSignatureType eniSignType;
    private EniElaborationStatus eniElaborationStatus;
    private String downloadUrl;
    // Signants
    private List<DocumentSigner> signers;
    private String datacaptura;
    private String darrerSegell;
    private Map<String, Object> metadata;    


    public void setDocumentLocation(DocumentLocation documentLocation) {
        this.documentLocation = documentLocation;
        this.eniDoc = (documentLocation.equals(DocumentLocation.NewDigitalArchive))?"1":"0";
    }

	@Override
	public String toString() {
		return "DocumentInfo [documentLocation=" + documentLocation + ", correctSafeKeeping=" + correctSafeKeeping
				+ ", validHierarchy=" + validHierarchy + ", verifiedHierarchy=" + verifiedHierarchy + ", documentCode="
				+ documentCode + ", documentName=" + documentName + ", documentType=" + documentType + ", hash=" + hash
				+ ", isPDF=" + printable + ", isCsvExclos=" + csvExclos + ", isAmagarBotoOriginal=" + amagarBotoOriginal
                + ", isHasPassword=" + hasPassword + ", isMalformatted=" + malformatted
                + ", eniDocumentId=" + eniDocumentId + ", eniOrgan=" + eniOrgan
				+ ", eniDocumentType=" + eniDocumentType + ", eniOrigin=" + eniOrigin + ", eniNtiVersion="
				+ eniNtiVersion + ", eniSignType=" + eniSignType + ", eniElaborationStatus=" + eniElaborationStatus
				+ ", downloadUrl=" + downloadUrl + ", darrerSegell=" + darrerSegell + ", signers=" + signers
				+ ", datacaptura=" + datacaptura + "]";
	}

}
