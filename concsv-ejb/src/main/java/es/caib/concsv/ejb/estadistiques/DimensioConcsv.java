package es.caib.concsv.ejb.estadistiques;

import com.fasterxml.jackson.annotation.JsonIgnore;
import es.caib.comanda.model.v1.estadistica.Dimensio;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DimensioConcsv implements Dimensio {

    @JsonIgnore
    private DimensioEnum tipus;
    private String valor;

    @Override
    public String getCodi() {
        return tipus.name();
    }

    public DimensioConcsv(DimensioEnum tipus, Double valor) {
        this.tipus = tipus;
        this.valor = valor != null ? String.valueOf(valor) : null;
    }

    public DimensioConcsv(DimensioEnum tipus, Long valor) {
        this.tipus = tipus;
        this.valor = valor != null ? String.valueOf(valor) : null;
    }

    public <E extends Enum<E>> DimensioConcsv(DimensioEnum tipus, E valor) {
        this.tipus = tipus;
        this.valor = valor != null ? valor.name() : null;
    }

}
