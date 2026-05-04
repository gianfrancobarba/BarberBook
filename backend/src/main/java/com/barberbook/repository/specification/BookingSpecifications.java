package com.barberbook.repository.specification;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.Prenotazione;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Specification Pattern (JPA Criteria API):
 * permette di comporre query dinamiche per i filtri delle dashboard.
 */
public class BookingSpecifications {

    private BookingSpecifications() {}

    /** Filtra per cliente (storico CLR) */
    public static Specification<Prenotazione> byClient(Long clientId) {
        return (root, query, cb) ->
            cb.equal(root.get("client").get("id"), clientId);
    }

    /** Filtra per stato (RF_CLR_3) */
    public static Specification<Prenotazione> byStatus(BookingStatus status) {
        return (root, query, cb) ->
            cb.equal(root.get("status"), status);
    }

    /** Filtra per giorno specifico (dashboard BAR) */
    public static Specification<Prenotazione> byDate(LocalDate date) {
        return (root, query, cb) ->
            cb.between(
                root.get("startTime"),
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX)
            );
    }

    /** Filtra per settimana (dashboard settimanale BAR) */
    public static Specification<Prenotazione> byWeek(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        return (root, query, cb) ->
            cb.between(
                root.get("startTime"),
                weekStart.atStartOfDay(),
                weekEnd.atTime(LocalTime.MAX)
            );
    }

    /** Filtra per poltrona (dashboard per poltrona) */
    public static Specification<Prenotazione> byChair(Long chairId) {
        return (root, query, cb) ->
            cb.equal(root.get("poltrona").get("id"), chairId);
    }

    /** Solo prenotazioni future (per homepage CLR) */
    public static Specification<Prenotazione> upcoming() {
        return (root, query, cb) ->
            cb.greaterThan(root.get("startTime"), LocalDateTime.now());
    }

    /** Esclude stati non rilevanti nella dashboard BAR */
    public static Specification<Prenotazione> notCancelledOrRejected() {
        return (root, query, cb) ->
            root.get("status").in(BookingStatus.IN_ATTESA, BookingStatus.ACCETTATA, BookingStatus.PASSATA);
    }

    /** Solo prenotazioni future confermate (homepage CLR) */
    public static Specification<Prenotazione> upcomingConfirmed() {
        return upcoming().and(byStatus(BookingStatus.ACCETTATA));
    }
}
