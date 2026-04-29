package com.barberbook.service;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.enums.ScheduleType;
import com.barberbook.domain.event.*;
import com.barberbook.domain.model.*;
import com.barberbook.dto.request.*;
import com.barberbook.dto.response.BookingResponseDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.UnauthorizedOperationException;
import com.barberbook.mapper.BookingMapper;
import com.barberbook.repository.FasciaOrariaRepository;
import com.barberbook.repository.PoltronaRepository;
import com.barberbook.repository.PrenotazioneRepository;
import com.barberbook.repository.ServizioRepository;
import com.barberbook.service.validation.BookingValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Test: BookingService (Facade)")
class BookingServiceTest {

    @Mock PrenotazioneRepository prenotazioneRepository;
    @Mock PoltronaRepository poltronaRepository;
    @Mock ServizioRepository servizioRepository;
    @Mock FasciaOrariaRepository fasciaOrariaRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock BookingMapper bookingMapper;
    @Mock List<BookingValidator> validators;

    @InjectMocks BookingService bookingService;

    private Poltrona chair;
    private Servizio service;
    private User client;

    @BeforeEach
    void setUp() {
        chair = Poltrona.builder().id(1L).nome("P1").attiva(true).build();
        service = Servizio.builder().id(1L).nome("Taglio").durataMinuti(30).attivo(true).build();
        client = new ClienteRegistrato(); 
        client.setId(10L);
    }

    @Test
    @DisplayName("createRequest - successo")
    void createRequestSuccess() {
        BookingRequestDto dto = new BookingRequestDto(1L, 1L, LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(chair));
        when(servizioRepository.findByIdAndAttivoTrue(1L)).thenReturn(Optional.of(service));
        when(prenotazioneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(bookingMapper.toDto(any())).thenReturn(mock(BookingResponseDto.class));

        bookingService.createRequest(dto, client);

        verify(prenotazioneRepository).save(argThat(p -> 
            p.getStatus() == BookingStatus.IN_ATTESA && 
            p.getClient().equals(client)
        ));
        verify(eventPublisher).publishEvent(any(BookingRequestCreatedEvent.class));
    }

    @Test
    @DisplayName("acceptRequest - cambia stato e pubblica evento")
    void acceptRequestSuccess() {
        Prenotazione booking = Prenotazione.builder().status(BookingStatus.IN_ATTESA).build();
        when(prenotazioneRepository.findById(1L)).thenReturn(Optional.of(booking));

        bookingService.acceptRequest(1L);

        assertEquals(BookingStatus.ACCETTATA, booking.getStatus());
        verify(eventPublisher).publishEvent(any(BookingAcceptedEvent.class));
    }

    @Test
    @DisplayName("cancelByClient - fallisce se non proprietario")
    void cancelByClientUnauthorized() {
        User owner = new ClienteRegistrato(); owner.setId(10L);
        User hacker = new ClienteRegistrato(); hacker.setId(99L);
        Prenotazione booking = Prenotazione.builder().client(owner).status(BookingStatus.ACCETTATA).build();
        
        when(prenotazioneRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(UnauthorizedOperationException.class, 
            () -> bookingService.cancelByClient(1L, "Scusa", hacker));
    }

    @Test
    @DisplayName("cancelByClient - successo")
    void cancelByClientSuccess() {
        Prenotazione booking = Prenotazione.builder().client(client).status(BookingStatus.ACCETTATA).build();
        when(prenotazioneRepository.findById(1L)).thenReturn(Optional.of(booking));

        bookingService.cancelByClient(1L, "Imprevisto", client);

        assertEquals(BookingStatus.ANNULLATA, booking.getStatus());
        assertEquals("Imprevisto", booking.getCancellationReason());
        verify(eventPublisher).publishEvent(any(BookingCancelledByClientEvent.class));
    }

    @Test
    @DisplayName("createDirect - status direttamente ACCETTATA")
    void createDirectSuccess() {
        DirectBookingRequestDto dto = new DirectBookingRequestDto(1L, 1L, LocalDate.now(), LocalTime.of(15, 0), "Mario", "Rossi", null);
        
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(chair));
        when(servizioRepository.findByIdAndAttivoTrue(1L)).thenReturn(Optional.of(service));
        when(prenotazioneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        bookingService.createDirect(dto);

        verify(prenotazioneRepository).save(argThat(p -> p.getStatus() == BookingStatus.ACCETTATA));
        verifyNoInteractions(eventPublisher); // BAR non notifica sé stesso
    }
}
