package com.barberbook.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequestDto(
    @NotBlank String nome,
    @NotBlank String cognome,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    String telefono
) {}
