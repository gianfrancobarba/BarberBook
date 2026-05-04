package com.barberbook.domain.model;

import com.barberbook.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Rappresenta una notifica persistita nel sistema.
 */
@Entity
@Table(name = "notifications",
       indexes = {
           @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
           @Index(name = "idx_notification_read", columnList = "letta"),
           @Index(name = "idx_notification_created", columnList = "created_at")
       })
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Notifica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType tipo;

    @Column(nullable = false, length = 200)
    private String titolo;

    @Column(nullable = false, length = 1000)
    private String messaggio;

    @Column(nullable = false)
    @Builder.Default
    private boolean letta = false;

    @Column(name = "booking_id")
    private Long bookingId;          // riferimento alla prenotazione correlata

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
