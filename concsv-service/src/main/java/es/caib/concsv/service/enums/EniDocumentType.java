package es.caib.concsv.service.enums;

public enum EniDocumentType {
    TD01(){ public String getDescription(String lng) { return (lng.equals("es")?"Resolución":"Resolució"); } }
    , TD02(){ public String getDescription(String lng) { return (lng.equals("es")?"Acuerdo":"Acord"); } }
    , TD03(){ public String getDescription(String lng) { return (lng.equals("es")?"Contrato":"Contracte"); } }
    , TD04(){ public String getDescription(String lng) { return (lng.equals("es")?"Convenio":"Conveni"); } }
    , TD05(){ public String getDescription(String lng) { return (lng.equals("es")?"Declaración":"Declaració"); } }
    , TD06(){ public String getDescription(String lng) { return (lng.equals("es")?"Comunicación":"Comunicació"); } }
    , TD07(){ public String getDescription(String lng) { return (lng.equals("es")?"Notificación":"Notificació"); } }
    , TD08(){ public String getDescription(String lng) { return (lng.equals("es")?"Publicación":"Publicació"); } }
    , TD09(){ public String getDescription(String lng) { return (lng.equals("es")?"Acuse de recibo":"Acusament de rebut"); } }
    , TD10(){ public String getDescription(String lng) { return (lng.equals("es")?"Acta":"Acta"); } }
    , TD11(){ public String getDescription(String lng) { return (lng.equals("es")?"Certificado":"Certificat"); } }
    , TD12(){ public String getDescription(String lng) { return (lng.equals("es")?"Diligencia":"Diligència"); } }
    , TD13(){ public String getDescription(String lng) { return (lng.equals("es")?"Informe":"Informe"); } }
    , TD14(){ public String getDescription(String lng) { return (lng.equals("es")?"Solicitud":"Solicitud"); } }
    , TD15(){ public String getDescription(String lng) { return (lng.equals("es")?"Denuncia":"Denúncia"); } }
    , TD16(){ public String getDescription(String lng) { return (lng.equals("es")?"Alegación":"Alegació"); } }
    , TD17(){ public String getDescription(String lng) { return (lng.equals("es")?"Recursos":"Recursos"); } }
    , TD18(){ public String getDescription(String lng) { return (lng.equals("es")?"Comunicación ciudadano":"Comunicació ciutadà"); } }
    , TD19(){ public String getDescription(String lng) { return (lng.equals("es")?"Factura":"Factura"); } }
    , TD20(){ public String getDescription(String lng) { return (lng.equals("es")?"Otros incautados":"Altres incautats"); } }
    , TD99(){ public String getDescription(String lng) { return (lng.equals("es")?"Otros":"Altres"); } }
    ;
    public abstract String getDescription(String lng);
}
