package com.barberbook.dto.response;

import java.util.List;

/**
 * Risposta contenente gli slot disponibili per una specifica poltrona.
 */
public record AvailabilityResponseDto(
    Long chairId,
    String chairName,
    List<TimeSlotDto> availableSlots
) {}
