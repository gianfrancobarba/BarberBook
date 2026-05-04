package com.barberbook.dto.response;

import java.util.List;

/**
 * Risposta per la homepage del cliente (CLR).
 */
public record ClientHomepageDto(
    String clientName,
    List<BookingResponseDto> upcomingBookings,
    int totalBookings,
    long unreadNotifications
) {}
