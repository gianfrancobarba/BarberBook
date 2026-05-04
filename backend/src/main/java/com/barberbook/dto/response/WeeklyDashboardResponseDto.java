package com.barberbook.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Risposta per la dashboard settimanale completa.
 */
public record WeeklyDashboardResponseDto(
    LocalDate weekStart,
    LocalDate weekEnd,
    List<DayScheduleDto> days
) {}
