package com.barberbook.service.strategy;

import com.barberbook.domain.model.AvailabilityContext;
import com.barberbook.domain.model.TimeSlot;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Strategy Pattern: definisce il contratto per il calcolo degli slot liberi.
 * Implementazioni diverse possono essere iniettate da Spring senza modificare
 * il Service che le usa.
 */
public interface AvailabilityStrategy {
    /**
     * Calcola gli slot temporali liberi per una poltrona in un dato giorno.
     *
     * @param date            il giorno per cui calcolare la disponibilità
     * @param serviceDuration la durata del servizio selezionato
     * @param context         orari di apertura, pause, prenotazioni esistenti
     * @return lista ordinata di slot disponibili (può essere vuota)
     */
    List<TimeSlot> calculateAvailableSlots(
        LocalDate date,
        Duration serviceDuration,
        AvailabilityContext context
    );
}
