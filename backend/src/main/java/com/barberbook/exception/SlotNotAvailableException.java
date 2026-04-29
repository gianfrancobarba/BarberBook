package com.barberbook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Eccezione lanciata quando uno slot temporale richiesto non è più disponibile.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SlotNotAvailableException extends RuntimeException {
    public SlotNotAvailableException(String message) {
        super(message);
    }
}
