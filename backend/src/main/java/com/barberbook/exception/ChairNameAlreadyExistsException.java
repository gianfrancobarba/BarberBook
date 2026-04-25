package com.barberbook.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ChairNameAlreadyExistsException extends RuntimeException {
    public ChairNameAlreadyExistsException(String message) {
        super(message);
    }
}
