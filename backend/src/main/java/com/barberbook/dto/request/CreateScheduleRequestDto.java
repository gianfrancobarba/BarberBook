package com.barberbook.dto.request;

import com.barberbook.domain.enums.ScheduleType;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Richiesta per la creazione di una nuova fascia oraria o pausa.
 */
public record CreateScheduleRequestDto(
    @NotNull Long chairId,
    @NotNull DayOfWeek giornoSettimana,
    @NotNull LocalTime oraInizio,
    @NotNull LocalTime oraFine,
    @NotNull ScheduleType tipo
) {}
