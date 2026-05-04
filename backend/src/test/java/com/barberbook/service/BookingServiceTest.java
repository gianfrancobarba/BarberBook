package com.barberbook.service;

import com.barberbook.domain.enums.BookingStatus;
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
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("createRequest - ritorna DTO non null")
    void createRequestReturnsDto() {
        BookingRequestDto dto = new BookingRequestDto(1L, 1L, LocalDate.now().plusDays(1), LocalTime.of(10, 0));
        BookingResponseDto expected = mock(BookingResponseDto.class);

        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(chair));
        when(servizioRepository.findByIdAndAttivoTrue(1L)).thenReturn(Optional.of(service));
        when(prenotazioneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(bookingMapper.toDto(any())).thenReturn(expected);

        BookingResponseDto result = bookingService.createRequest(dto, client);

        assertNotNull(result);
        assertSame(expected, result);
    }

    @Test
    @DisplayName("createGuestRequest - salva prenotazione senza client e pubblica evento")
    void createGuestRequestSuccess() {
        GuestBookingRequestDto dto = new GuestBookingRequestDto(
            1L, 1L, LocalDate.now().plusDays(1), LocalTime.of(11, 0),
            "Mario", "Rossi", "3331234567"
        );

        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(chair));
        when(servizioRepository.findByIdAndAttivoTrue(1L)).thenReturn(Optional.of(service));
        when(prenotazioneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(bookingMapper.toDto(any())).thenReturn(mock(BookingResponseDto.class));

        BookingResponseDto result = bookingService.createGuestRequest(dto);

        assertNotNull(result);
        verify(prenotazioneRepository).save(argThat(p ->
            p.getClient() == null &&
            p.getGuestData() != null &&
            p.getStatus() == BookingStatus.IN_ATTESA
        ));
        verify(eventPublisher).publishEvent(any(BookingRequestCreatedEvent.class));
    }

    @Test
    @DisplayName("rejectRequest - cambia stato a RIFIUTATA e pubblica evento")
    void rejectRequestSuccess() {
        Prenotazione booking = Prenotazione.builder().status(BookingStatus.IN_ATTESA).build();
        when(prenotazioneRepository.findById(2L)).thenReturn(Optional.of(booking));

        bookingService.rejectRequest(2L);

        assertEquals(BookingStatus.RIFIUTATA, booking.getStatus());
        assertNotNull(booking.getUpdatedAt());
        verify(prenotazioneRepository).save(booking);
        verify(eventPublisher).publishEvent(any(BookingRejectedEvent.class));
    }

    @Test
    @DisplayName("cancelByBarber - cambia stato a ANNULLATA e pubblica evento")
    void cancelByBarberSuccess() {
        Prenotazione booking = Prenotazione.builder().status(BookingStatus.ACCETTATA).build();
        when(prenotazioneRepository.findById(3L)).thenReturn(Optional.of(booking));

        bookingService.cancelByBarber(3L);

        assertEquals(BookingStatus.ANNULLATA, booking.getStatus());
        assertNotNull(booking.getUpdatedAt());
        verify(prenotazioneRepository).save(booking);
        verify(eventPublisher).publishEvent(any(BookingCancelledByBarberEvent.class));
    }

    @Test
    @DisplayName("acceptRequest - imposta updatedAt")
    void acceptRequestSetsUpdatedAt() {
        Prenotazione booking = Prenotazione.builder().status(BookingStatus.IN_ATTESA).build();
        when(prenotazioneRepository.findById(4L)).thenReturn(Optional.of(booking));

        bookingService.acceptRequest(4L);

        assertNotNull(booking.getUpdatedAt());
    }

    @Test
    @DisplayName("getPendingRequests - delega a repository e mapper")
    void getPendingRequestsDelegatesToRepository() {
        Prenotazione p = Prenotazione.builder().status(BookingStatus.IN_ATTESA).build();
        when(prenotazioneRepository.findByStatusOrderByCreatedAtAsc(BookingStatus.IN_ATTESA))
            .thenReturn(List.of(p));
        when(bookingMapper.toDtoList(any())).thenReturn(List.of(mock(BookingResponseDto.class)));

        List<BookingResponseDto> result = bookingService.getPendingRequests();

        assertEquals(1, result.size());
        verify(prenotazioneRepository).findByStatusOrderByCreatedAtAsc(BookingStatus.IN_ATTESA);
    }

    @Test
    @DisplayName("getClientBookings - ritorna prenotazioni del cliente")
    void getClientBookingsDelegatesToRepository() {
        when(prenotazioneRepository.findByClientOrderByStartTimeDesc(client)).thenReturn(List.of());
        when(bookingMapper.toDtoList(any())).thenReturn(List.of());

        List<BookingResponseDto> result = bookingService.getClientBookings(client);

        assertNotNull(result);
        verify(prenotazioneRepository).findByClientOrderByStartTimeDesc(client);
    }

    @Test
    @DisplayName("findOrThrow - lancia eccezione se prenotazione non trovata")
    void findOrThrowNotFound() {
        when(prenotazioneRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookingService.acceptRequest(99L));
    }
    @Test void updateSuccess() { 
        UpdateBookingRequestDto dto = new UpdateBookingRequestDto(1L, 1L, LocalDate.now(), LocalTime.of(16,0)); 
        Prenotazione booking = Prenotazione.builder().status(BookingStatus.IN_ATTESA).startTime(LocalDateTime.now()).poltrona(chair).servizio(service).build(); 
        when(prenotazioneRepository.findById(1L)).thenReturn(Optional.of(booking)); 
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(chair)); 
        when(servizioRepository.findByIdAndAttivoTrue(1L)).thenReturn(Optional.of(service)); 
        when(prenotazioneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]); 
        when(bookingMapper.toDto(any())).thenReturn(mock(BookingResponseDto.class)); 
        bookingService.update(1L, dto); 
        verify(prenotazioneRepository).save(any()); 
    }

    @Test void updateWithNullValuesSuccess() {
        UpdateBookingRequestDto dto = new UpdateBookingRequestDto(null, null, null, null); 
        Prenotazione booking = Prenotazione.builder().status(BookingStatus.IN_ATTESA).startTime(LocalDateTime.now()).poltrona(chair).servizio(service).build(); 
        when(prenotazioneRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(prenotazioneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]); 
        when(bookingMapper.toDto(any())).thenReturn(mock(BookingResponseDto.class)); 
        bookingService.update(1L, dto); 
        verify(prenotazioneRepository).save(any()); 
    }

    @Test
    @DisplayName("rebook: lancia eccezione se non proprietario")
    void rebookUnauthorized() {
        User hacker = new ClienteRegistrato(); hacker.setId(99L);
        Prenotazione past = Prenotazione.builder().client(client).poltrona(chair).servizio(service).build();
        
        when(prenotazioneRepository.findById(1L)).thenReturn(Optional.of(past));

        assertThrows(UnauthorizedOperationException.class, 
            () -> bookingService.rebook(1L, LocalDate.now(), LocalTime.now(), hacker));
    }

    @Test
    @DisplayName("rebook: crea nuova richiesta basata sulla precedente")
    void rebookSuccess() {
        Prenotazione past = Prenotazione.builder().client(client).poltrona(chair).servizio(service).build();
        LocalDate nextWeek = LocalDate.now().plusWeeks(1);
        LocalTime sameTime = LocalTime.of(10, 0);

        when(prenotazioneRepository.findById(1L)).thenReturn(Optional.of(past));
        when(poltronaRepository.findByIdAndAttivaTrue(1L)).thenReturn(Optional.of(chair));
        when(servizioRepository.findByIdAndAttivoTrue(1L)).thenReturn(Optional.of(service));
        when(prenotazioneRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(bookingMapper.toDto(any())).thenReturn(mock(BookingResponseDto.class));

        bookingService.rebook(1L, nextWeek, sameTime, client);

        verify(prenotazioneRepository, times(1)).save(argThat(p -> 
            p.getPoltrona().equals(chair) && 
            p.getServizio().equals(service) &&
            p.getStartTime().toLocalDate().equals(nextWeek)
        ));
    }
}
