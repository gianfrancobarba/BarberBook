package com.barberbook.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Rappresenta l'agenda di una singola poltrona per un giorno specifico.
 */
public record ChairDayScheduleDto(
    Long chairId,
    String chairName,
    LocalDate date,
    List<BookingResponseDto> bookings,
    List<TimeSlotDto> freeSlots
) {}
