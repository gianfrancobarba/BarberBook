package com.barberbook.dto.response;

public record AuthResponseDto(
    String accessToken,
    String tokenType,
    long expiresIn,
    String refreshTokenRaw,
    UserResponseDto user
) {}
