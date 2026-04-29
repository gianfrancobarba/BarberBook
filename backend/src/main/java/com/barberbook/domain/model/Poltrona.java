package com.barberbook.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chairs",
       uniqueConstraints = @UniqueConstraint(columnNames = "nome", name = "uq_chair_nome"))
public class Poltrona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(nullable = false)
    private boolean attiva = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public Poltrona() {}

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public boolean isAttiva() { return attiva; }
    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setAttiva(boolean attiva) { this.attiva = attiva; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static PoltronaBuilder builder() {
        return new PoltronaBuilder();
    }

    public static class PoltronaBuilder {
        private Long id;
        private String nome;
        private boolean attiva = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public PoltronaBuilder id(Long id) { this.id = id; return this; }
        public PoltronaBuilder nome(String nome) { this.nome = nome; return this; }
        public PoltronaBuilder attiva(boolean attiva) { this.attiva = attiva; return this; }
        public PoltronaBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public PoltronaBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Poltrona build() {
            Poltrona p = new Poltrona();
            p.id = this.id;
            p.nome = this.nome;
            p.attiva = this.attiva;
            p.createdAt = this.createdAt;
            p.updatedAt = this.updatedAt;
            return p;
        }
    }
}
