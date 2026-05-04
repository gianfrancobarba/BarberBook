package com.barberbook.domain.model;

import com.barberbook.domain.enums.ScheduleType;
import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "schedules",
       indexes = {
           @Index(name = "idx_schedule_chair_day", columnList = "chair_id, giorno_settimana"),
           @Index(name = "idx_schedule_type", columnList = "tipo")
       })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
