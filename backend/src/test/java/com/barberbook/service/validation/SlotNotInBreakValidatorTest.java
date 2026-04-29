package com.barberbook.service.validation;

import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.exception.BookingValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SlotNotInBreakValidatorTest {

    private final SlotNotInBreakValidator validator = new SlotNotInBreakValidator();

    @Test
    @DisplayName("Slot che collide con una pausa lancia eccezione")
    void slotInBreakThrowsException() {
        FasciaOraria pause = FasciaOraria.builder()
            .oraInizio(LocalTime.of(13, 0))
            .oraFine(LocalTime.of(15, 0))
            .build();
        
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 12, 45);
        LocalDateTime end = start.plusMinutes(30); // Finisce alle 13:15, collide!

        BookingValidationRequest req = new BookingValidationRequest(null, null, null, List.of(pause), start, end, null, null);
        assertThrows(BookingValidationException.class, () -> validator.validate(req));
    }

    @Test
    @DisplayName("Slot adiacente alla pausa passa validazione")
    void slotAdjacentToBreakPasses() {
        FasciaOraria pause = FasciaOraria.builder()
            .oraInizio(LocalTime.of(13, 0))
            .oraFine(LocalTime.of(15, 0))
            .build();
        
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 12, 30);
        LocalDateTime end = start.plusMinutes(30); // Finisce esattamente alle 13:00

        BookingValidationRequest req = new BookingValidationRequest(null, null, null, List.of(pause), start, end, null, null);
        assertDoesNotThrow(() -> validator.validate(req));
    }
}
