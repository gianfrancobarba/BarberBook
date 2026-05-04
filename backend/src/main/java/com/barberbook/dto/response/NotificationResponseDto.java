package com.barberbook.dto.response;

import com.barberbook.domain.enums.NotificationType;
import java.time.LocalDateTime;

/**
 * DTO per la visualizzazione delle notifiche nello storico (frontend).
 */
public record NotificationResponseDto(
    Long id,
    NotificationType tipo,
    String titolo,
    String messaggio,
    boolean letta,
    Long bookingId,
    LocalDateTime createdAt
) {}
