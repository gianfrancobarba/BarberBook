package com.barberbook.service;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.event.*;
import com.barberbook.domain.model.*;
import com.barberbook.dto.response.NotificationPushDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.UnauthorizedOperationException;
import com.barberbook.mapper.NotificationMapper;
import com.barberbook.repository.NotificaRepository;
import com.barberbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificaRepository notificaRepository;
    @Mock private SseEmitterRegistry sseRegistry;
    @Mock private UserRepository userRepository;
    @Mock private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationService notificationService;

    private User client;
    private User barber;
    private Prenotazione booking;

    @BeforeEach
    void setUp() {
        client = new ClienteRegistrato();
        client.setId(1L);
        client.setNome("Mario");
        client.setCognome("Rossi");

        barber = new Barbiere();
        barber.setId(2L);
        barber.setNome("Tony");
        barber.setCognome("Barber");
        
        Servizio servizio = Servizio.builder().nome("Taglio").build();
        Poltrona poltrona = Poltrona.builder().nome("P1").build();
        
        booking = Prenotazione.builder()
            .id(100L)
            .client(client)
            .servizio(servizio)
            .poltrona(poltrona)
            .startTime(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("onBookingRequestCreated: crea notifica e la invia via SSE")
    void onBookingRequestCreated_success() {
        when(userRepository.findByRuolo(UserRole.BARBER)).thenReturn(List.of(barber));
        NotificationPushDto pushDto = mock(NotificationPushDto.class);
        when(notificationMapper.toPushDto(any())).thenReturn(pushDto);

        notificationService.onBookingRequestCreated(new BookingRequestCreatedEvent(this, booking));

        verify(notificaRepository, times(1)).save(any(Notifica.class));
        verify(sseRegistry, times(1)).pushToAllBarbers(pushDto);
    }

    @Test
    @DisplayName("onBookingAccepted: invia notifica al cliente")
    void onBookingAccepted_success() {
        NotificationPushDto pushDto = mock(NotificationPushDto.class);
        when(notificationMapper.toPushDto(any())).thenReturn(pushDto);

        notificationService.onBookingAccepted(new BookingAcceptedEvent(this, booking));

        verify(notificaRepository, times(1)).save(any(Notifica.class));
        verify(sseRegistry, times(1)).pushToUser(eq(client.getId()), eq(pushDto));
    }

    @Test
    @DisplayName("onBookingAccepted: nessuna notifica se Guest")
    void onBookingAccepted_guest_noNotification() {
        booking.setClient(null);
        notificationService.onBookingAccepted(new BookingAcceptedEvent(this, booking));
        verify(notificaRepository, never()).save(any());
    }

    @Test
    @DisplayName("onBookingRejected: invia notifica al cliente")
    void onBookingRejected_success() {
        NotificationPushDto pushDto = mock(NotificationPushDto.class);
        when(notificationMapper.toPushDto(any())).thenReturn(pushDto);

        notificationService.onBookingRejected(new BookingRejectedEvent(this, booking));

        verify(notificaRepository, times(1)).save(any(Notifica.class));
        verify(sseRegistry, times(1)).pushToUser(eq(client.getId()), eq(pushDto));
    }

    @Test
    @DisplayName("onBookingCancelledByClient: invia notifica al barbiere")
    void onBookingCancelledByClient_success() {
        when(userRepository.findByRuolo(UserRole.BARBER)).thenReturn(List.of(barber));
        NotificationPushDto pushDto = mock(NotificationPushDto.class);
        when(notificationMapper.toPushDto(any())).thenReturn(pushDto);

        notificationService.onBookingCancelledByClient(new BookingCancelledByClientEvent(this, booking, "Test Reason"));

        verify(notificaRepository, times(1)).save(any(Notifica.class));
        verify(sseRegistry, times(1)).pushToAllBarbers(pushDto);
    }

    @Test
    @DisplayName("onBookingCancelledByBarber: invia notifica al cliente")
    void onBookingCancelledByBarber_success() {
        NotificationPushDto pushDto = mock(NotificationPushDto.class);
        when(notificationMapper.toPushDto(any())).thenReturn(pushDto);

        notificationService.onBookingCancelledByBarber(new BookingCancelledByBarberEvent(this, booking));

        verify(notificaRepository, times(1)).save(any(Notifica.class));
        verify(sseRegistry, times(1)).pushToUser(eq(client.getId()), eq(pushDto));
    }

    @Test
    @DisplayName("markAsRead: segna come letta se proprietario")
    void markAsRead_success() {
        Notifica n = Notifica.builder().id(10L).destinatario(client).letta(false).build();
        when(notificaRepository.findById(10L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(10L, client.getId());

        assertTrue(n.isLetta());
        verify(notificaRepository).save(n);
    }

    @Test
    @DisplayName("markAsRead: errore se non proprietario")
    void markAsRead_unauthorized() {
        Notifica n = Notifica.builder().id(10L).destinatario(barber).build();
        when(notificaRepository.findById(10L)).thenReturn(Optional.of(n));

        assertThrows(UnauthorizedOperationException.class, () -> notificationService.markAsRead(10L, client.getId()));
    }

    @Test
    @DisplayName("markAsRead: errore se non esiste")
    void markAsRead_notFound() {
        when(notificaRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> notificationService.markAsRead(99L, client.getId()));
    }
}
