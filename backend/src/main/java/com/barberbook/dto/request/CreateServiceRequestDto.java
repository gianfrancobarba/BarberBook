package com.barberbook.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateServiceRequestDto(
    @NotBlank @Size(max = 100) String nome,
    @Size(max = 500) String descrizione,
    @NotNull @Min(1) Integer durataMinuti,
    @NotNull @DecimalMin("0.00") BigDecimal prezzo
) {}
