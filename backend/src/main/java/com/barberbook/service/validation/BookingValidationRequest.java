package com.barberbook.service.validation;

import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Poltrona;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.Servizio;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Context object per la catena di validazione delle prenotazioni.
 */
public record BookingValidationRequest(
    Poltrona chair,
    Servizio service,
    FasciaOraria schedule,
    List<FasciaOraria> breaks,
    LocalDateTime startTime,
    LocalDateTime endTime,
    List<Prenotazione> existingBookings,
    Long excludeBookingId    // null per nuova prenotazione; usato per modifica
) {}
