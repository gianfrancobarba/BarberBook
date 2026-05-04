package com.barberbook.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "chairs",
       uniqueConstraints = @UniqueConstraint(columnNames = "nome", name = "uq_chair_nome"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Poltrona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(nullable = false)
    @Builder.Default
    private boolean attiva = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;
}
