package com.barberbook.service.validation;

import com.barberbook.domain.model.Prenotazione;
import com.barberbook.exception.SlotNotAvailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NoOverlapValidatorTest {

    private final NoOverlapValidator validator = new NoOverlapValidator();

    @Test
    @DisplayName("Sovrapposizione con prenotazione esistente lancia eccezione")
    void overlapThrowsException() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(30);

        Prenotazione existing = Prenotazione.builder()
            .id(100L)
            .startTime(start.minusMinutes(15))
            .endTime(start.plusMinutes(15)) // Si sovrappone per 15 min
            .build();

        BookingValidationRequest req = new BookingValidationRequest(null, null, null, List.of(), start, end, List.of(existing), null);
        assertThrows(SlotNotAvailableException.class, () -> validator.validate(req));
    }

    @Test
    @DisplayName("Esclusione ID proprio in fase di modifica")
    void excludeSelfPasses() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(30);

        Prenotazione self = Prenotazione.builder()
            .id(100L)
            .startTime(start)
            .endTime(end)
            .build();

        // Stesso slot, ma passo excludeBookingId = 100L
        BookingValidationRequest req = new BookingValidationRequest(null, null, null, List.of(), start, end, List.of(self), 100L);
        assertDoesNotThrow(() -> validator.validate(req));
    }
}
