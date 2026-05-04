package com.barberbook.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "services")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private Integer durataMinuti;

    @Column(nullable = false, precision = 8, scale = 2)
    @DecimalMin("0.00")
    private BigDecimal prezzo;

    @Column(nullable = false)
    @Builder.Default
    private boolean attivo = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
