package com.barberbook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequestDto(
    @Size(min = 1, max = 100) String nome,
    @Size(min = 1, max = 100) String cognome,
    @Email String email,
    @Size(max = 20) String telefono
) {}
