package com.barberbook.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("EmailAlreadyExistsException ritorna 400")
    void handleEmailAlreadyExists() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("Email duplicata");
        ResponseEntity<Map<String, Object>> response = handler.handleEmailAlreadyExists(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email duplicata", response.getBody().get("message"));
    }

    @Test
    @DisplayName("AuthExceptions ritornano 401")
    void handleAuthExceptions() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Credenziali errate");
        ResponseEntity<Map<String, Object>> response = handler.handleAuthExceptions(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenziali errate", response.getBody().get("message"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException ritorna 400 con mappa dettagli")
    void handleValidationExceptions() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
            new FieldError("dto", "email", "Email non valida")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidationExceptions(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, String> details = (Map<String, String>) response.getBody().get("details");
        assertNotNull(details);
        assertEquals("Email non valida", details.get("email"));
    }

    @Test
    @DisplayName("Eccezione generica ritorna 500")
    void handleGenericException() {
        Exception ex = new Exception("Eccezione imprevista");
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("Errore interno del server", response.getBody().get("message"));
    }
}
