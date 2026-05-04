package com.barberbook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Richiesta di avvio recupero password tramite email.
 */
public record ForgotPasswordRequestDto(
    @Email(message = "Formato email non valido")
    @NotBlank(message = "L'email è obbligatoria")
    String email
) {}
