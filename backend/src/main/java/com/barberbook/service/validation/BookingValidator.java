package com.barberbook.service.validation;

/**
 * Interfaccia base per i componenti della catena di validazione prenotazioni.
 */
public interface BookingValidator {
    void validate(BookingValidationRequest request);
}
