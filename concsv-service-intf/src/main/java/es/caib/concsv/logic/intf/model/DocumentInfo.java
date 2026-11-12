package es.caib.concsv.logic.intf.model;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import es.caib.concsv.logic.intf.enums.DocumentLocation;
import es.caib.concsv.logic.intf.enums.EniDocumentType;
import es.caib.concsv.logic.intf.enums.EniElaborationStatus;
import es.caib.concsv.logic.intf.enums.EniSignatureType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DocumentInfo implements Serializable {

    private DocumentLocation documentLocation;
    @Setter(AccessLevel.NONE)
    private String eniDoc;
    private Boolean correctSafeKeeping;
    private Boolean validHierarchy;
    private Boolean verifiedHierarchy;
	/** Correspon amb uuid */
    private String documentCode;
    private String documentName;
    private String documentType;
    private EniDocumentType eniDocumentType;
    private String extensionFormato;
	private String hash;
    private Boolean printable;
    private Boolean csvExclos;
	private Boolean hasPassword;
    private Boolean malformatted;
	/** El PDF és un portafoli (catàleg amb {@code /Collection}). Es detecta en llegir-ne les signatures. */
	private Boolean portafoli;
	/** El PDF conté fitxers adjunts (incrustats o en anotacions). Un portafoli també en conté. */
	private Boolean fitxersAdjunts;
	/**
	 * No es genera la còpia imprimible perquè el PDF és un portafoli o té adjunts, segons la propietat
	 * {@code imprimible.exclusio}. El calcula el {@code HashService} a cada consulta.
	 */
	private Boolean imprimibleExclos;
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

    //Trasllat (document mogut)
    private String destinoTraslado;
    private String nuevaLocalizacion;
    private String fechaTraslado;

    public void setDocumentLocation(DocumentLocation documentLocation) {
        this.documentLocation = documentLocation;
        this.eniDoc = (documentLocation.equals(DocumentLocation.NewDigitalArchive))?"1":"0";
    }

	public String getCodi() {
		return this.hash != null ? this.hash : this.documentCode;
	}

	@Override
	public String toString() {
		return "DocumentInfo [documentLocation=" + documentLocation + ", correctSafeKeeping=" + correctSafeKeeping
				+ ", validHierarchy=" + validHierarchy + ", verifiedHierarchy=" + verifiedHierarchy + ", documentCode="
				+ documentCode + ", documentName=" + documentName + ", documentType=" + documentType + ", hash=" + hash
				+ ", isPDF=" + printable + ", isCsvExclos=" + csvExclos
                + ", isHasPassword=" + hasPassword + ", isMalformatted=" + malformatted
                + ", portafoli=" + portafoli + ", fitxersAdjunts=" + fitxersAdjunts
                + ", imprimibleExclos=" + imprimibleExclos
                + ", eniDocumentId=" + eniDocumentId + ", eniOrgan=" + eniOrgan
				+ ", eniDocumentType=" + eniDocumentType + ", eniOrigin=" + eniOrigin + ", eniNtiVersion="
				+ eniNtiVersion + ", eniSignType=" + eniSignType + ", eniElaborationStatus=" + eniElaborationStatus
				+ ", downloadUrl=" + downloadUrl + ", darrerSegell=" + darrerSegell + ", signers=" + signers
				+ ", datacaptura=" + datacaptura + "]";
	}

}
