package com.barberbook.mapper;

import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.User;
import com.barberbook.dto.request.RegisterRequestDto;
import com.barberbook.dto.response.UserResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponseDto toDto(User user);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ruolo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    ClienteRegistrato toEntity(RegisterRequestDto dto);
}
