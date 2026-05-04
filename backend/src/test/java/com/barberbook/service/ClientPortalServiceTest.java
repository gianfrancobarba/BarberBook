package com.barberbook.service;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.dto.response.BookingResponseDto;
import com.barberbook.mapper.BookingMapper;
import com.barberbook.repository.PrenotazioneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientPortalServiceTest {

    @Mock private PrenotazioneRepository prenotazioneRepository;
    @Mock private BookingMapper bookingMapper;
    @InjectMocks private ClientPortalService clientPortalService;

    private ClienteRegistrato client;

    @BeforeEach
    void setUp() {
        client = new ClienteRegistrato();
        client.setId(1L);
        client.setNome("Mario");
    }

    @Test
    @DisplayName("getUpcomingBookings: ritorna lista dei prossimi appuntamenti")
    void getUpcomingBookings_success() {
        when(prenotazioneRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(new Prenotazione()));
        
        BookingResponseDto dto = new BookingResponseDto(1L, 1L, "P1", 1L, "S1", 30, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30), BookingStatus.ACCETTATA, "Mario", false, null, null, LocalDateTime.now());
        when(bookingMapper.toDto(any())).thenReturn(dto);

        List<BookingResponseDto> result = clientPortalService.getUpcomingBookings(client);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("getBookingHistory: ritorna lo storico completo")
    void getBookingHistory_success() {
        when(prenotazioneRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<BookingResponseDto> result = clientPortalService.getBookingHistory(client);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("getBookingsByStatus: filtra correttamente per lo stato fornito")
    void getBookingsByStatus_success() {
        when(prenotazioneRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(Collections.emptyList());

        List<BookingResponseDto> result = clientPortalService.getBookingsByStatus(client, BookingStatus.ACCETTATA);

        assertEquals(0, result.size());
    }
}
