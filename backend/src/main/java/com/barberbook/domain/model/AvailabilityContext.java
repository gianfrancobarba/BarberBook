package com.barberbook.domain.model;

import java.util.List;

/**
 * Raggruppa tutti i dati necessari all'algoritmo di calcolo.
 * Pattern: Parameter Object — riduce la firma del metodo Strategy.
 */
public record AvailabilityContext(
    FasciaOraria schedule,              // fascia APERTURA per quel giorno
    List<FasciaOraria> breaks,          // fasce PAUSA per quella poltrona in quel giorno
    List<Prenotazione> existingBookings // prenotazioni IN_ATTESA + ACCETTATA in quel giorno
) {}
