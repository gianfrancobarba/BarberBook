package com.barberbook.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Risposta per la dashboard giornaliera completa (tutte le poltrone).
 */
public record DailyDashboardResponseDto(
    LocalDate date,
    List<ChairDayScheduleDto> chairs
) {}
