package com.barberbook.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
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

    public Prenotazione() {}

    public Long getId() { return id; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public Poltrona getPoltrona() { return poltrona; }

    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setPoltrona(Poltrona poltrona) { this.poltrona = poltrona; }

    public static PrenotazioneBuilder builder() {
        return new PrenotazioneBuilder();
    }

    public static class PrenotazioneBuilder {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Poltrona poltrona;

        public PrenotazioneBuilder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public PrenotazioneBuilder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public PrenotazioneBuilder poltrona(Poltrona poltrona) { this.poltrona = poltrona; return this; }

        public Prenotazione build() {
            Prenotazione p = new Prenotazione();
            p.startTime = this.startTime;
            p.endTime = this.endTime;
            p.poltrona = this.poltrona;
            return p;
        }
    }
}
