package com.barberbook.service.validation;

import com.barberbook.domain.model.Poltrona;
import com.barberbook.exception.BookingValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ChairActiveValidatorTest {

    private final ChairActiveValidator validator = new ChairActiveValidator();

    @Test
    @DisplayName("Poltrona inattiva lancia eccezione")
    void inactiveChairThrowsException() {
        Poltrona chair = Poltrona.builder().attiva(false).build();
        BookingValidationRequest req = new BookingValidationRequest(chair, null, null, null, null, null, null, null);
        
        assertThrows(BookingValidationException.class, () -> validator.validate(req));
    }

    @Test
    @DisplayName("Poltrona attiva passa validazione")
    void activeChairPasses() {
        Poltrona chair = Poltrona.builder().attiva(true).build();
        BookingValidationRequest req = new BookingValidationRequest(chair, null, null, null, null, null, null, null);
        
        assertDoesNotThrow(() -> validator.validate(req));
    }
}
