package com.barberbook.exception;

/**
 * Eccezione lanciata quando si tenta una transizione di stato non valida per una prenotazione.
 */
public class InvalidBookingTransitionException extends RuntimeException {
    public InvalidBookingTransitionException(String message) {
        super(message);
    }
}
