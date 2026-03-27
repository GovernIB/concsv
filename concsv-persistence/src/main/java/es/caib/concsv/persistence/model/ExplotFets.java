package es.caib.concsv.persistence.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ExplotFets {

    private EnviamentTipus tipus;
    private EnviamentOrigen origen;
    // Totals
    private Long correcte;
    private Long codiInvalid;
    private Long error;
    // Temps mig
    private Long tempsMigCorrecte;

}
