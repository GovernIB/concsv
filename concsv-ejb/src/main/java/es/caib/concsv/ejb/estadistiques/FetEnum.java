package es.caib.concsv.ejb.estadistiques;

import lombok.Getter;

@Getter
public enum FetEnum {

    CORRECTE("Correctes", "Peticions que han funcionat correctament"),
    CODI_INVALID("Codi invalid", "Peticions que no han procedir per rebre un codi invalid o inexistent"),
    ERROR("Erronis", "Peticions que han provocat un error en la aplicació."),
    TEMPS_MITJ_CORRECTE("Temps mitj correcte", "Temps mitjs per peticio correcte");

    private final String nom;
    private final String descripcio;
    
    FetEnum(String nom, String descripcio) {
        this.nom = nom;
        this.descripcio = descripcio;
    }

}
