package com.barberbook.mapper;

import com.barberbook.domain.model.Servizio;
import com.barberbook.dto.request.CreateServiceRequestDto;
import com.barberbook.dto.response.ServiceResponseDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServizioMapper {
    ServiceResponseDto toDto(Servizio servizio);
    List<ServiceResponseDto> toDtoList(List<Servizio> servizi);
    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "attivo", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    Servizio toEntity(CreateServiceRequestDto dto);
}
