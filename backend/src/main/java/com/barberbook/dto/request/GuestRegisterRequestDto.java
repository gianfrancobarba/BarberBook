package com.barberbook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Richiesta di conversione di un ospite (CLG) in cliente registrato (CLR) 
 * a seguito di una prenotazione effettuata.
 */
public record GuestRegisterRequestDto(
    @NotNull(message = "L'ID della prenotazione è obbligatorio")
    Long bookingId,

    @Email(message = "Formato email non valido")
    @NotBlank(message = "L'email è obbligatoria")
    String email,

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve avere almeno 8 caratteri")
    String password
) {}
