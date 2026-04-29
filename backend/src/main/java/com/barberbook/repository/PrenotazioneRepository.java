package com.barberbook.repository;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long>, JpaSpecificationExecutor<Prenotazione> {

    // Prenotazioni attive (IN_ATTESA + ACCETTATA) per una poltrona in un giorno
    @Query("""
        SELECT p FROM Prenotazione p
        WHERE p.poltrona.id = :chairId
          AND CAST(p.startTime AS date) = :date
          AND p.status IN ('IN_ATTESA', 'ACCETTATA')
        ORDER BY p.startTime
        """)
    List<Prenotazione> findActiveBookingsByChairAndDate(
        @Param("chairId") Long chairId,
        @Param("date") LocalDate date
    );

    // Verifica sovrapposizione per assertSlotIsAvailable
    @Query("""
        SELECT COUNT(p) > 0 FROM Prenotazione p
        WHERE p.poltrona.id = :chairId
          AND p.status IN ('IN_ATTESA', 'ACCETTATA')
          AND p.startTime < :endTime
          AND p.endTime > :startTime
        """)
    boolean existsActiveBookingInSlot(
        @Param("chairId") Long chairId,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    // Prenotazioni in attesa (per area notifiche BAR)
    List<Prenotazione> findByStatusOrderByCreatedAtAsc(BookingStatus status);

    // Prenotazioni del cliente (con filtro opzionale per stato)
    List<Prenotazione> findByClientOrderByStartTimeDesc(User client);

    // Prenotazioni future confermate del cliente (homepage CLR)
    @Query("""
        SELECT p FROM Prenotazione p
        WHERE p.client.id = :clientId
          AND p.status = 'ACCETTATA'
          AND p.startTime > :now
        ORDER BY p.startTime ASC
        """)
    List<Prenotazione> findUpcomingConfirmedByClient(
        @Param("clientId") Long clientId,
        @Param("now") LocalDateTime now
    );

    // Dashboard giornaliera BAR
    @Query("""
        SELECT p FROM Prenotazione p
        WHERE p.poltrona.id = :chairId
          AND CAST(p.startTime AS date) = :date
          AND p.status NOT IN ('RIFIUTATA', 'ANNULLATA')
        ORDER BY p.startTime
        """)
    List<Prenotazione> findDailyBookingsByChair(
        @Param("chairId") Long chairId,
        @Param("date") LocalDate date
    );

    // Per lo scheduler PASSATA
    List<Prenotazione> findByStatusAndEndTimeBefore(BookingStatus status, LocalDateTime now);
}
