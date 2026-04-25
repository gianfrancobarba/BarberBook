package com.barberbook.mapper;

import com.barberbook.domain.model.Poltrona;
import com.barberbook.dto.request.CreateChairRequestDto;
import com.barberbook.dto.response.ChairResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PoltronaMapper {
    ChairResponseDto toDto(Poltrona poltrona);
    List<ChairResponseDto> toDtoList(List<Poltrona> poltrone);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "attiva", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Poltrona toEntity(CreateChairRequestDto dto);
}
