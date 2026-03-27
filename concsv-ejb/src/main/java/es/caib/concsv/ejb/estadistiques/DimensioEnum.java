package es.caib.concsv.ejb.estadistiques;

import lombok.Getter;

@Getter
public enum DimensioEnum {
    TIP ("Tipus", "Tipus de comunicació. L'accio demanada (original, imprimible, eni o metadades)"),
    ORI ("Origen", "Lloc des d'on s'ha creat la comunicació/notificació: interfície web o API Rest");

    private final String nom;
    private final String descripcio;

    DimensioEnum(String nom, String descripcio) {
        this.nom = nom;
        this.descripcio = descripcio;
    }

}
