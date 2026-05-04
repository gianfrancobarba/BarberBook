package com.barberbook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Richiesta di aggiornamento dei dati del profilo personale.
 */
public record UpdateProfileRequestDto(
    @Size(max = 100, message = "Il nome può avere massimo 100 caratteri")
    String nome,

    @Size(max = 100, message = "Il cognome può avere massimo 100 caratteri")
    String cognome,

    @Email(message = "Formato email non valido")
    @Size(max = 255)
    String email,

    @Pattern(regexp = "\\+?[0-9\\s\\-]{8,20}", message = "Numero di telefono non valido")
    String telefono
) {}
