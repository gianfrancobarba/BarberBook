package com.barberbook.domain.model;

import com.barberbook.domain.enums.ScheduleType;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedules",
       indexes = {
           @Index(name = "idx_schedule_chair_day", columnList = "chair_id, giorno_settimana"),
           @Index(name = "idx_schedule_type", columnList = "tipo")
       })
public class FasciaOraria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chair_id", nullable = false)
    private Poltrona poltrona;

    @Enumerated(EnumType.STRING)
    @Column(name = "giorno_settimana", nullable = false)
    private DayOfWeek giornoSettimana;

    @Column(name = "ora_inizio", nullable = false)
    private LocalTime oraInizio;

    @Column(name = "ora_fine", nullable = false)
    private LocalTime oraFine;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private ScheduleType tipo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public FasciaOraria() {}

    public Long getId() { return id; }
    public Poltrona getPoltrona() { return poltrona; }
    public DayOfWeek getGiornoSettimana() { return giornoSettimana; }
    public LocalTime getOraInizio() { return oraInizio; }
    public LocalTime getOraFine() { return oraFine; }
    public ScheduleType getTipo() { return tipo; }

    public void setPoltrona(Poltrona poltrona) { this.poltrona = poltrona; }
    public void setGiornoSettimana(DayOfWeek giornoSettimana) { this.giornoSettimana = giornoSettimana; }
    public void setOraInizio(LocalTime oraInizio) { this.oraInizio = oraInizio; }
    public void setOraFine(LocalTime oraFine) { this.oraFine = oraFine; }
    public void setTipo(ScheduleType tipo) { this.tipo = tipo; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static FasciaOrariaBuilder builder() {
        return new FasciaOrariaBuilder();
    }

    public static class FasciaOrariaBuilder {
        private Poltrona poltrona;
        private DayOfWeek giornoSettimana;
        private LocalTime oraInizio;
        private LocalTime oraFine;
        private ScheduleType tipo;
        private LocalDateTime createdAt;

        public FasciaOrariaBuilder poltrona(Poltrona poltrona) { this.poltrona = poltrona; return this; }
        public FasciaOrariaBuilder giornoSettimana(DayOfWeek giornoSettimana) { this.giornoSettimana = giornoSettimana; return this; }
        public FasciaOrariaBuilder oraInizio(LocalTime oraInizio) { this.oraInizio = oraInizio; return this; }
        public FasciaOrariaBuilder oraFine(LocalTime oraFine) { this.oraFine = oraFine; return this; }
        public FasciaOrariaBuilder tipo(ScheduleType tipo) { this.tipo = tipo; return this; }
        public FasciaOrariaBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public FasciaOraria build() {
            FasciaOraria f = new FasciaOraria();
            f.poltrona = this.poltrona;
            f.giornoSettimana = this.giornoSettimana;
            f.oraInizio = this.oraInizio;
            f.oraFine = this.oraFine;
            f.tipo = this.tipo;
            f.createdAt = this.createdAt;
            return f;
        }
    }
}
