package com.barberbook.service.validation;

import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.exception.BookingValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SlotWithinScheduleValidatorTest {

    private final SlotWithinScheduleValidator validator = new SlotWithinScheduleValidator();

    @Test
    @DisplayName("Nessuna fascia oraria (chiuso) lancia eccezione")
    void noScheduleThrowsException() {
        BookingValidationRequest req = new BookingValidationRequest(null, null, null, null, null, null, null, null);
        assertThrows(BookingValidationException.class, () -> validator.validate(req));
    }

    @Test
    @DisplayName("Slot fuori orario lancia eccezione")
    void slotOutOfRangeThrowsException() {
        FasciaOraria schedule = FasciaOraria.builder()
            .oraInizio(LocalTime.of(9, 0))
            .oraFine(LocalTime.of(18, 0))
            .build();
        
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 8, 30); // Prima dell'apertura
        LocalDateTime end = start.plusMinutes(30);

        BookingValidationRequest req = new BookingValidationRequest(null, null, schedule, null, start, end, null, null);
        assertThrows(BookingValidationException.class, () -> validator.validate(req));
    }

    @Test
    @DisplayName("Slot in orario passa validazione")
    void slotInRangePasses() {
        FasciaOraria schedule = FasciaOraria.builder()
            .oraInizio(LocalTime.of(9, 0))
            .oraFine(LocalTime.of(18, 0))
            .build();
        
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 10, 0);
        LocalDateTime end = start.plusMinutes(30);

        BookingValidationRequest req = new BookingValidationRequest(null, null, schedule, null, start, end, null, null);
        assertDoesNotThrow(() -> validator.validate(req));
    }
}
