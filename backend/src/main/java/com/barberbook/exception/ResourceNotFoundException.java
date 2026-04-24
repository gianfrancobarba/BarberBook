package com.barberbook.exception;

/**
 * Eccezione base dell'applicazione — risorsa non trovata.
 * Mappata a 404 Not Found dal GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
