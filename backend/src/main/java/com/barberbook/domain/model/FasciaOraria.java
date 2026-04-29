package com.barberbook.domain.model;

import com.barberbook.domain.enums.ScheduleType;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedules",
       indexes = {
           @Index(name = "idx_schedule_chair_day", columnList = "chair_id, giorno_settimana"),
           @Index(name = "idx_schedule_type", columnList = "tipo")
       })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
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
    private ScheduleType tipo;           // APERTURA | PAUSA

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
