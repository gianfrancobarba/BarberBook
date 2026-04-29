package com.barberbook.dto.response;

import com.barberbook.domain.enums.ScheduleType;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Risposta contenente i dettagli di una fascia oraria configurata.
 */
public record ScheduleResponseDto(
    Long id,
    Long chairId,
    String chairName,
    DayOfWeek giornoSettimana,
    LocalTime oraInizio,
    LocalTime oraFine,
    ScheduleType tipo
) {}
