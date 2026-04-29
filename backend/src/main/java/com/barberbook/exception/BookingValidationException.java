package com.barberbook.exception;

/**
 * Eccezione lanciata quando la validazione di una prenotazione fallisce.
 */
public class BookingValidationException extends RuntimeException {
    public BookingValidationException(String message) {
        super(message);
    }
}
