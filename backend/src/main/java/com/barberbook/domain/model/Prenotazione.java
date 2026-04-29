package com.barberbook.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entità Prenotazione (Stub per lo Sprint 4).
 * Verrà completata nello Sprint 5.
 */
@Entity
@Table(name = "bookings")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chair_id", nullable = false)
    private Poltrona poltrona;
    
    // Altri campi (cliente, servizio, stato) verranno aggiunti nello Sprint 5.
}
