package com.barberbook.dto.response;

import java.math.BigDecimal;

public record ServiceResponseDto(
    Long id,
    String nome,
    String descrizione,
    Integer durataMinuti,
    BigDecimal prezzo
) {}
