package com.barberbook.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

public record UpdateBookingRequestDto(
    Long chairId,
    Long serviceId,
    LocalDate date,
    LocalTime startTime
) {}
