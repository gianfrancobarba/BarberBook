package com.barberbook.dto.response;

import com.barberbook.domain.enums.BookingStatus;
import java.time.LocalDateTime;

public record BookingResponseDto(
    Long id,
    Long chairId,
    String chairName,
    Long serviceId,
    String serviceName,
    Integer serviceDurationMinutes,
    LocalDateTime startTime,
    LocalDateTime endTime,
    BookingStatus status,
    String customerName,
    boolean isGuest,
    String guestPhone,
    String cancellationReason,
    LocalDateTime createdAt
) {}
