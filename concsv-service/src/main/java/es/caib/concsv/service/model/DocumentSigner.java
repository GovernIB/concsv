package es.caib.concsv.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DocumentSigner {

    private String signerCN;
    private String signerOU;
    private String signerOU2;
    private String signerT;
    private String signerO;
    private String signerCIF;
    private String puesto;
    private String tipoCertificat;
    private String sistemaComponent;
    private String dataSignatura;
    private String dataSegell;
    private String idEuropeu;
    private boolean representat;
    private String representatRaoSocial;
    private String representatNifCif;
    private String representatDocument;
    private String reason;

	@Override
	public String toString() {
		return "DocumentSigner [signerCN=" + signerCN + ", signerOU=" + signerOU + ", signerOU2=" + signerOU2
				+ ", signerT=" + signerT + ", signerO=" + signerO + ", signerCIF=" + signerCIF + ", puesto=" + puesto
				+ ", tipoCertificat=" + tipoCertificat + ", sistemaComponent=" + sistemaComponent + ", dataSignatura="
				+ dataSignatura + ", reason=" + reason + "]";
	}

}
