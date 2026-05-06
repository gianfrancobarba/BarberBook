package com.barberbook.mapper;

import com.barberbook.domain.model.Servizio;
import com.barberbook.dto.request.CreateServiceRequestDto;
import com.barberbook.dto.response.ServiceResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServizioMapper {

    @Mapping(source = "durataMinuti", target = "durata")
    ServiceResponseDto toDto(Servizio servizio);

    List<ServiceResponseDto> toDtoList(List<Servizio> servizi);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "attivo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(source = "durata", target = "durataMinuti")
    Servizio toEntity(CreateServiceRequestDto dto);
}
