package com.barberbook.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Servizio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descrizione;

    @Column(nullable = false)
    @Min(1)
    private Integer durataMinuti;       // durata stimata in minuti (> 0)

    @Column(nullable = false, precision = 8, scale = 2)
    @DecimalMin("0.00")
    private BigDecimal prezzo;          // in EUR

    @Column(nullable = false)
    @Builder.Default
    private boolean attivo = true;      // soft-delete: false = eliminato

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
