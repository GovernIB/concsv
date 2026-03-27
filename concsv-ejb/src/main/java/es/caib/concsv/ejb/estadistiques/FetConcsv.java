package es.caib.concsv.ejb.estadistiques;

import com.fasterxml.jackson.annotation.JsonIgnore;
import es.caib.comanda.model.v1.estadistica.Fet;
import lombok.Getter;

@Getter
public class FetConcsv implements Fet {

    @JsonIgnore
    private FetEnum tipus;
    private Double valor;

    @Override
    public String getCodi() {
        return tipus.name();
    }

    public FetConcsv(FetEnum tipus, Double valor) {
        this.tipus = tipus;
        this.valor = valor;
    }

    public FetConcsv(FetEnum tipus, Long valor) {
        this.tipus = tipus;
        this.valor = valor != null ? valor.doubleValue() : null;
    }
}
