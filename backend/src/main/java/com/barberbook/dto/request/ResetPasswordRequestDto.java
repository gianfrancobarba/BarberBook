package com.barberbook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Richiesta di conferma reset password con nuovo valore.
 */
public record ResetPasswordRequestDto(
    @NotBlank(message = "Il token è obbligatorio")
    String token,

    @NotBlank(message = "La nuova password è obbligatoria")
    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    String newPassword
) {}
