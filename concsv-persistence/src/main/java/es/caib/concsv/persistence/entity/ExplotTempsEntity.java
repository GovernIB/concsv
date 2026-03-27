package es.caib.concsv.persistence.entity;


import es.caib.concsv.persistence.model.DiaSetmanaEnum;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.WeekFields;


@Entity
@Table(name = "csv_explot_temps")
@Getter
@Setter
@Builder
@AllArgsConstructor
public class ExplotTempsEntity extends BaseEntity implements Serializable {

	private static final long serialVersionUID = -2144138256112639860L;
    @Column(name = "data")
    private LocalDate data;
    @Column(name = "anualitat")
    private Integer anualitat;
    @Column(name = "mes")
    private Integer mes;
    @Column(name = "trimestre")
    private Integer trimestre;
    @Column(name = "setmana")
    private Integer setmana;
    @Column(name = "dia")
    private Integer dia;
    @Column(name = "dia_setmana")
    @Enumerated(EnumType.STRING)
    private DiaSetmanaEnum diaSetmana;

    public ExplotTempsEntity() {
        super();
        emplenarCamps(LocalDate.now());
    }

    public ExplotTempsEntity(LocalDate data) {
        super();
        emplenarCamps(data);
    }

    private void emplenarCamps(LocalDate data) {
        this.data = data;
        this.anualitat = data.getYear();
        this.trimestre = data.getMonthValue() / 3;
        this.mes = data.getMonthValue();
        this.setmana = data.get(WeekFields.ISO.weekOfWeekBasedYear());
        this.dia = data.getDayOfMonth();
        this.diaSetmana = DiaSetmanaEnum.valueOfData(data.getDayOfWeek().name());
    }
}