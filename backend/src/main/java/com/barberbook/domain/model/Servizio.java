package com.barberbook.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
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
    private boolean attivo = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public Servizio() {}

    public static ServizioBuilder builder() {
        return new ServizioBuilder();
    }

    public static class ServizioBuilder {
        private Long id;
        private Integer durataMinuti;
        private boolean attivo = true;
        public ServizioBuilder id(Long id) { this.id = id; return this; }
        public ServizioBuilder durataMinuti(Integer durataMinuti) { this.durataMinuti = durataMinuti; return this; }
        public ServizioBuilder attivo(boolean attivo) { this.attivo = attivo; return this; }
        public Servizio build() {
            Servizio s = new Servizio();
            s.id = this.id;
            s.durataMinuti = this.durataMinuti;
            s.attivo = this.attivo;
            return s;
        }
    }
    public Long getId() { return id; }
    public Integer getDurataMinuti() { return durataMinuti; }
}
