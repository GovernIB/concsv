package es.caib.concsv.persistence.entity;

import es.caib.concsv.persistence.model.ExplotFets;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "csv_explot_fet")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExplotFetsEntity extends BaseEntity implements Serializable {

	private static final long serialVersionUID = 2900135379128738307L;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "dimensio_id")
    protected ExplotDimensioEntity dimensio;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "temps_id")
    protected ExplotTempsEntity temps;
    // Totals
    @Column(name = "tot_correcte")
    private Long correcte;
    @Column(name = "tot_codi_invalid")
    private Long codiInvalid;
    @Column(name = "tot_error")
    private Long error;
    // Temps mig
    @Column(name = "temps_mig_correcte")
    private Long tempsMigCorrecte;

    public ExplotFetsEntity(ExplotDimensioEntity dimension, ExplotTempsEntity ete, ExplotFets fets) {
        this.dimensio = dimension;
        this.temps = ete;
        this.correcte = fets.getCorrecte();
        this.codiInvalid = fets.getCodiInvalid();
        this.error = fets.getError();
        this.tempsMigCorrecte = fets.getTempsMigCorrecte();
    }
}
