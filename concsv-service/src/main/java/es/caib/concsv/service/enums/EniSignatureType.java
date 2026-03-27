package es.caib.concsv.service.enums;

public enum EniSignatureType {
    TF01(){ public String getDescription() { return "CSV"; }}
    , TF02(){ public String getDescription() { return "XAdES internally detached signature"; }}
    , TF03(){ public String getDescription() { return "XAdES enveloped signature"; }}
    , TF04(){ public String getDescription() { return "CAdES detached/explicit signature"; }}
    , TF05(){ public String getDescription() { return "CAdES attached/implicit signature"; }}
    , TF06(){ public String getDescription() { return "Pades"; }};
    public abstract String getDescription();
}
