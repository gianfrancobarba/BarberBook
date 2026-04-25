package com.barberbook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateChairRequestDto(
    @NotBlank @Size(max = 100) String nome
) {}
