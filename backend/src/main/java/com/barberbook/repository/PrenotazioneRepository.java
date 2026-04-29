package com.barberbook.repository;

import com.barberbook.domain.model.Prenotazione;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository per le Prenotazioni (Stub per lo Sprint 4).
 * Verrà completato nello Sprint 5.
 */
public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {

    @Query("SELECT b FROM Prenotazione b WHERE b.poltrona.id = :chairId " +
           "AND b.startTime >= :startOfDay AND b.startTime < :endOfDay")
    List<Prenotazione> findActiveBookingsByChairAndDate(
            @Param("chairId") Long chairId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    default List<Prenotazione> findActiveBookingsByChairAndDate(Long chairId, LocalDate date) {
        return findActiveBookingsByChairAndDate(chairId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    @Query("SELECT COUNT(b) > 0 FROM Prenotazione b WHERE b.poltrona.id = :chairId " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean existsActiveBookingInSlot(
            @Param("chairId") Long chairId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
