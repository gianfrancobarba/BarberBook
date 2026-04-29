package com.barberbook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.LocalTime;

public record GuestBookingRequestDto(
    @NotNull Long chairId,
    @NotNull Long serviceId,
    @NotNull LocalDate date,
    @NotNull LocalTime startTime,
    @NotBlank String guestNome,
    @NotBlank String guestCognome,
    @NotBlank @Pattern(regexp = "\\+?[0-9\\s\\-]{8,15}") String guestTelefono
) {}
