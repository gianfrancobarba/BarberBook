package com.barberbook.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateServiceRequestDto(
    @Size(max = 100) String nome,
    @Size(max = 500) String descrizione,
    @Min(1) Integer durata,
    @DecimalMin("0.00") BigDecimal prezzo
) {}
