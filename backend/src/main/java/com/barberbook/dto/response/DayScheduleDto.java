package com.barberbook.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Rappresenta un giorno all'interno della dashboard settimanale.
 */
public record DayScheduleDto(
    LocalDate date,
    String dayName,
    List<ChairDayScheduleDto> chairs
) {}
