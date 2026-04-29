package com.barberbook.mapper;

import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.dto.response.ScheduleResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FasciaOrariaMapper {

    @Mapping(source = "poltrona.id", target = "chairId")
    @Mapping(source = "poltrona.nome", target = "chairName")
    ScheduleResponseDto toDto(FasciaOraria entity);

    List<ScheduleResponseDto> toDtoList(List<FasciaOraria> entities);
}
