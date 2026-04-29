package com.barberbook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record DirectBookingRequestDto(
    @NotNull Long chairId,
    @NotNull Long serviceId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotBlank String customerName,
    @NotBlank String customerSurname,
    String customerPhone
) {}
