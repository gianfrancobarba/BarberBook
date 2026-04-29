package com.barberbook.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingRequestDto(
    @NotNull Long chairId,
    @NotNull Long serviceId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime
) {}
