package com.barberbook.dto.response;

/**
 * Rappresenta uno slot temporale disponibile.
 */
public record TimeSlotDto(
    String start,   // "HH:mm"
    String end      // "HH:mm"
) {}
