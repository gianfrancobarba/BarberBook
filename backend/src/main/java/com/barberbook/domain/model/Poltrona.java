package com.barberbook.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chairs",
       uniqueConstraints = @UniqueConstraint(columnNames = "nome", name = "uq_chair_nome"))
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Poltrona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;                // nome personalizzato (es. "Poltrona Mario")

    @Column(nullable = false)
    @Builder.Default
    private boolean attiva = true;      // soft-delete

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
