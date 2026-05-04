package com.barberbook.dto.response;

import com.barberbook.domain.enums.NotificationType;
import java.time.LocalDateTime;

/**
 * DTO leggero per l'invio di notifiche via SSE (push real-time).
 */
public record NotificationPushDto(
    Long id,
    NotificationType tipo,
    String titolo,
    String messaggio,
    Long bookingId,
    LocalDateTime createdAt
) {}
