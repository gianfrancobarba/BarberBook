package com.barberbook.service.strategy;

import com.barberbook.domain.model.AvailabilityContext;
import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.TimeSlot;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementazione standard dell'algoritmo di calcolo slot.
 * Utilizza una granularità di 15 minuti per la generazione degli slot di inizio.
 */
@Component
@Primary
public class StandardAvailabilityStrategy implements AvailabilityStrategy {

    private static final int SLOT_GRANULARITY_MINUTES = 15;

    @Override
    public List<TimeSlot> calculateAvailableSlots(
            LocalDate date,
            Duration serviceDuration,
            AvailabilityContext context) {

        // 1. Nessuna fascia di apertura per questo giorno → salone chiuso
        if (context.schedule() == null) {
            return Collections.emptyList();
        }

        // 2. Genera tutti gli slot teorici del giorno con la granularità di 15 min
        List<TimeSlot> allSlots = generateAllSlots(
            context.schedule().getOraInizio(),
            context.schedule().getOraFine(),
            serviceDuration
        );

        // 3. Filtra gli slot che cadono in una pausa
        List<TimeSlot> notInBreaks = filterBreaks(allSlots, context.breaks());

        // 4. Filtra gli slot che si sovrappongono con prenotazioni esistenti
        return filterBookedSlots(notInBreaks, context.existingBookings());
    }

    /**
     * Genera tutti gli slot del giorno con step di SLOT_GRANULARITY_MINUTES.
     * Uno slot è incluso solo se TERMINA entro l'orario di chiusura.
     */
    private List<TimeSlot> generateAllSlots(LocalTime openTime, LocalTime closeTime,
                                             Duration duration) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = openTime;

        while (true) {
            // Se current + duration sfora la mezzanotte o l'orario di chiusura
            // LocalTime.plus può lanciare eccezioni o fare wrap-around.
            // Gestiamo il limite di chiusura in modo sicuro.
            
            try {
                LocalTime slotEnd = current.plus(duration);
                // Se lo slot finisce dopo l'orario di chiusura o è tornato indietro (wrap-around)
                if (slotEnd.isAfter(closeTime) || slotEnd.isBefore(current)) break;
                
                slots.add(new TimeSlot(current, slotEnd));
                current = current.plusMinutes(SLOT_GRANULARITY_MINUTES);
                
                // Se il nuovo inizio è oltre l'orario di chiusura (meno la granularità)
                if (!current.isBefore(closeTime)) break;
            } catch (Exception e) {
                break;
            }
        }

        return slots;
    }

    /**
     * Rimuove gli slot che si sovrappongono con almeno una fascia di pausa.
     */
    private List<TimeSlot> filterBreaks(List<TimeSlot> slots, List<FasciaOraria> breaks) {
        if (breaks == null || breaks.isEmpty()) return slots;
        
        return slots.stream()
            .filter(slot -> breaks.stream().noneMatch(b ->
                slot.overlapsWith(b.getOraInizio(), b.getOraFine())
            ))
            .collect(Collectors.toList());
    }

    /**
     * Rimuove gli slot che si sovrappongono con prenotazioni esistenti.
     */
    private List<TimeSlot> filterBookedSlots(List<TimeSlot> slots,
                                              List<Prenotazione> bookings) {
        if (bookings == null || bookings.isEmpty()) return slots;
        
        return slots.stream()
            .filter(slot -> bookings.stream().noneMatch(b ->
                slot.overlapsWith(
                    b.getStartTime().toLocalTime(),
                    b.getEndTime().toLocalTime()
                )
            ))
            .collect(Collectors.toList());
    }
}
