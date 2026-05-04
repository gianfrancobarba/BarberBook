package com.barberbook.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Richiesta di riprenotazione rapida basata su un appuntamento passato.
 */
public record RebookRequestDto(
    @NotNull(message = "La data è obbligatoria")
    LocalDate date,

    @NotNull(message = "L'orario di inizio è obbligatorio")
    LocalTime startTime
) {}
