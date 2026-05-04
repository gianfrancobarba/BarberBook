package com.barberbook.mapper;

import com.barberbook.domain.model.Notifica;
import com.barberbook.dto.response.NotificationPushDto;
import com.barberbook.dto.response.NotificationResponseDto;
import org.mapstruct.Mapper;

/**
 * Mapper per la conversione dell'entità Notifica in DTO.
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponseDto toDto(Notifica notifica);

    NotificationPushDto toPushDto(Notifica notifica);
}
