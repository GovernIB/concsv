package es.caib.concsv.logic.intf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ExplotDimensio {

    private final EnviamentTipus tipus;
    private final EnviamentOrigen origen;

}
