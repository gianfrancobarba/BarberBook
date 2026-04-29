package com.barberbook.exception;

/**
 * Eccezione lanciata quando un utente tenta di eseguire un'operazione per la quale non ha i permessi.
 */
public class UnauthorizedOperationException extends RuntimeException {
    public UnauthorizedOperationException(String message) {
        super(message);
    }
}
