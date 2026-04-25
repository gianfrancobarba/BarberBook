package com.barberbook.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateChairRequestDto(
    @NotBlank @Size(max = 100) String nome
) {}
