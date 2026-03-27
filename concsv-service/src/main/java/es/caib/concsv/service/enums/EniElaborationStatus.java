package es.caib.concsv.service.enums;

public enum EniElaborationStatus {
    EE01(){ public String getDescription(String lng) {
        return (lng.equals("es")?"Original":"Original"); } }
    , EE02(){ public String getDescription(String lng) {
        return (lng.equals("es")?"Copia electrónica auténtica con cambio de formato":"Còpia electrònica autèntica amb canvi de format"); } }
    , EE03(){ public String getDescription(String lng) {
        return (lng.equals("es")?"Copia electrónica auténtica de documento papel":"Còpia electrònica autèntica de document paper"); } }
    , EE04(){ public String getDescription(String lng) {
        return (lng.equals("es")?"Copia electrónica parcial auténtica":"Còpia electrònica parcial autèntica"); } }
    , EE99(){ public String getDescription(String lng) {
        return (lng.equals("es")?"Otros":"Altres"); } };
    public abstract String getDescription(String lng);
}
