package com.barberbook.mapper;

import com.barberbook.domain.model.Prenotazione;
import com.barberbook.dto.response.BookingResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "chairId", source = "poltrona.id")
    @Mapping(target = "chairName", source = "poltrona.nome")
    @Mapping(target = "serviceId", source = "servizio.id")
    @Mapping(target = "serviceName", source = "servizio.nome")
    @Mapping(target = "serviceDurationMinutes", source = "servizio.durataMinuti")
    @Mapping(target = "customerName", expression = "java(entity.getCustomerDisplayName())")
    @Mapping(target = "isGuest", expression = "java(entity.getClient() == null)")
    @Mapping(target = "guestPhone", source = "guestData.telefono")
    BookingResponseDto toDto(Prenotazione entity);

    List<BookingResponseDto> toDtoList(List<Prenotazione> entities);
}
