package com.barberbook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Eccezione lanciata quando un intervallo temporale non è valido (es. inizio >= fine).
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTimeRangeException extends RuntimeException {
    public InvalidTimeRangeException(String message) {
        super(message);
    }
}
