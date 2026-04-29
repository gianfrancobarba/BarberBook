package com.barberbook.domain.model;

import com.barberbook.domain.enums.BookingStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings",
       indexes = {
           @Index(name = "idx_booking_chair_date", columnList = "chair_id, start_time"),
           @Index(name = "idx_booking_client", columnList = "client_id"),
           @Index(name = "idx_booking_status", columnList = "status")
       })
public class Prenotazione {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chair_id", nullable = false)
    private Poltrona poltrona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private Servizio servizio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")   // nullable: null per CLG (ospiti)
    private User client;

    @Embedded
    private GuestData guestData;        // valorizzato per CLG, null per CLR

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;      // calcolato: startTime + servizio.durataMinuti

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.IN_ATTESA;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;

    @Version
    private Long version;               // Optimistic Locking

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public Prenotazione() {}

    // Getters
    public Long getId() { return id; }
    public Poltrona getPoltrona() { return poltrona; }
    public Servizio getServizio() { return servizio; }
    public User getClient() { return client; }
    public GuestData getGuestData() { return guestData; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BookingStatus getStatus() { return status; }
    public String getCancellationReason() { return cancellationReason; }
    public Long getVersion() { return version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setPoltrona(Poltrona poltrona) { this.poltrona = poltrona; }
    public void setServizio(Servizio servizio) { this.servizio = servizio; }
    public void setClient(User client) { this.client = client; }
    public void setGuestData(GuestData guestData) { this.guestData = guestData; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // --- Metodi di utilità ---
    public boolean overlaps(LocalDateTime start, LocalDateTime end) {
        return this.startTime.isBefore(end) && start.isBefore(this.endTime);
    }

    public String getCustomerDisplayName() {
        if (client != null) {
            return client.getNome() + " " + client.getCognome();
        }
        if (guestData != null) {
            return guestData.getNome() + " " + guestData.getCognome() + " (ospite)";
        }
        return "Cliente sconosciuto";
    }

    public static PrenotazioneBuilder builder() {
        return new PrenotazioneBuilder();
    }

    public static class PrenotazioneBuilder {
        private Long id;
        private Poltrona poltrona;
        private Servizio servizio;
        private User client;
        private GuestData guestData;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BookingStatus status = BookingStatus.IN_ATTESA;
        private LocalDateTime createdAt;

        public PrenotazioneBuilder id(Long id) { this.id = id; return this; }
        public PrenotazioneBuilder poltrona(Poltrona poltrona) { this.poltrona = poltrona; return this; }
        public PrenotazioneBuilder servizio(Servizio servizio) { this.servizio = servizio; return this; }
        public PrenotazioneBuilder client(User client) { this.client = client; return this; }
        public PrenotazioneBuilder guestData(GuestData guestData) { this.guestData = guestData; return this; }
        public PrenotazioneBuilder startTime(LocalDateTime startTime) { this.startTime = startTime; return this; }
        public PrenotazioneBuilder endTime(LocalDateTime endTime) { this.endTime = endTime; return this; }
        public PrenotazioneBuilder status(BookingStatus status) { this.status = status; return this; }
        public PrenotazioneBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public Prenotazione build() {
            Prenotazione p = new Prenotazione();
            p.id = this.id;
            p.poltrona = this.poltrona;
            p.servizio = this.servizio;
            p.client = this.client;
            p.guestData = this.guestData;
            p.startTime = this.startTime;
            p.endTime = this.endTime;
            p.status = this.status;
            p.createdAt = this.createdAt;
            return p;
        }
    }
}
