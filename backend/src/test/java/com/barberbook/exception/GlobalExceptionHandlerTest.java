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
    @DisplayName("InvalidTimeRangeException ritorna 400")
    void handleInvalidTimeRange() {
        InvalidTimeRangeException ex = new InvalidTimeRangeException("Range invalido");
        ResponseEntity<Map<String, Object>> response = handler.handleInvalidTimeRange(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("BookingValidationException ritorna 400")
    void handleBookingValidation() {
        BookingValidationException ex = new BookingValidationException("Validazione fallita");
        ResponseEntity<Map<String, Object>> response = handler.handleBookingValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("UnauthorizedOperationException ritorna 403")
    void handleUnauthorizedOperation() {
        UnauthorizedOperationException ex = new UnauthorizedOperationException("Non autorizzato");
        ResponseEntity<Map<String, Object>> response = handler.handleUnauthorizedOperation(ex);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
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
    @DisplayName("ChairNameAlreadyExistsException ritorna 409")
    void handleChairConflict() {
        ChairNameAlreadyExistsException ex = new ChairNameAlreadyExistsException("Conflitto");
        ResponseEntity<Map<String, Object>> response = handler.handleChairConflict(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("InvalidBookingTransitionException ritorna 409")
    void handleBookingTransition() {
        InvalidBookingTransitionException ex = new InvalidBookingTransitionException("Transizione invalida");
        ResponseEntity<Map<String, Object>> response = handler.handleBookingTransition(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("SlotNotAvailableException ritorna 409")
    void handleSlotNotAvailable() {
        SlotNotAvailableException ex = new SlotNotAvailableException("Slot occupato");
        ResponseEntity<Map<String, Object>> response = handler.handleSlotNotAvailable(ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }

    @Test
    @DisplayName("ResourceNotFoundException ritorna 404")
    void handleResourceNotFound() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Non trovato");
        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(ex);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
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
