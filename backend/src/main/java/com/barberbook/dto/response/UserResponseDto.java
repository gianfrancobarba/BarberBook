package com.barberbook.dto.response;

import com.barberbook.domain.enums.UserRole;

public record UserResponseDto(
    Long id,
    String nome,
    String cognome,
    String email,
    String telefono,
    UserRole ruolo
) {}
