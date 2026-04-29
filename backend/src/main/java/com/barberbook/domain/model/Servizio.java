package com.barberbook.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

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

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getDescrizione() { return descrizione; }
    public Integer getDurataMinuti() { return durataMinuti; }
    public BigDecimal getPrezzo() { return prezzo; }
    public boolean isAttivo() { return attivo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(Long id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }
    public void setDurataMinuti(Integer durataMinuti) { this.durataMinuti = durataMinuti; }
    public void setPrezzo(BigDecimal prezzo) { this.prezzo = prezzo; }
    public void setAttivo(boolean attivo) { this.attivo = attivo; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static ServizioBuilder builder() {
        return new ServizioBuilder();
    }

    public static class ServizioBuilder {
        private Long id;
        private String nome;
        private String descrizione;
        private Integer durataMinuti;
        private BigDecimal prezzo;
        private boolean attivo = true;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ServizioBuilder id(Long id) { this.id = id; return this; }
        public ServizioBuilder nome(String nome) { this.nome = nome; return this; }
        public ServizioBuilder descrizione(String descrizione) { this.descrizione = descrizione; return this; }
        public ServizioBuilder durataMinuti(Integer durataMinuti) { this.durataMinuti = durataMinuti; return this; }
        public ServizioBuilder prezzo(BigDecimal prezzo) { this.prezzo = prezzo; return this; }
        public ServizioBuilder attivo(boolean attivo) { this.attivo = attivo; return this; }
        public ServizioBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public ServizioBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Servizio build() {
            Servizio s = new Servizio();
            s.id = this.id;
            s.nome = this.nome;
            s.descrizione = this.descrizione;
            s.durataMinuti = this.durataMinuti;
            s.prezzo = this.prezzo;
            s.attivo = this.attivo;
            s.createdAt = this.createdAt;
            s.updatedAt = this.updatedAt;
            return s;
        }
    }
}
