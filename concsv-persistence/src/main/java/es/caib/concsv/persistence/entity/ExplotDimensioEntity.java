package es.caib.concsv.persistence.entity;


import lombok.*;
import es.caib.concsv.persistence.model.EnviamentOrigen;
import es.caib.concsv.persistence.model.EnviamentTipus;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "csv_explot_dim", uniqueConstraints = {@UniqueConstraint(name = "csv_explot_dim_uk", columnNames = {"tipus", "origen"})})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ExplotDimensioEntity extends BaseEntity implements Serializable  {

	private static final long serialVersionUID = 2900135379128738307L;
    @Column(name = "tipus")
    @Enumerated(EnumType.STRING)
    private EnviamentTipus tipus;
    @Column(name = "origen")
    @Enumerated(EnumType.STRING)
    private EnviamentOrigen origen;
}