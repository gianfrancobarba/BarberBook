package com.barberbook.service;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.event.*;
import com.barberbook.domain.model.Notifica;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.User;
import com.barberbook.dto.response.NotificationResponseDto;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.UnauthorizedOperationException;
import com.barberbook.mapper.NotificationMapper;
import com.barberbook.repository.NotificaRepository;
import com.barberbook.repository.UserRepository;
import com.barberbook.service.factory.NotificationFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Observer Pattern: ascolta gli Spring ApplicationEvent pubblicati da BookingService
 * e genera/persiste/pusha le notifiche in modo completamente disaccoppiato.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificaRepository notificaRepository;
    private final SseEmitterRegistry sseRegistry;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;

    // -------------------------------------------------------
    // OBSERVER: nuova richiesta → notifica al BAR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingRequestCreated(BookingRequestCreatedEvent event) {
        Prenotazione booking = event.getBooking();
        User barber = getBarber();  // nel sistema: unico account BARBER

        Notifica n = NotificationFactory.createNewRequestNotification(booking, barber);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToAllBarbers(notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: accettazione → notifica al CLR (non al CLG)
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingAccepted(BookingAcceptedEvent event) {
        Prenotazione booking = event.getBooking();

        if (booking.getClient() == null) {
            // CLG: nessuna notifica in-app — Tony contatta per telefono
            return;
        }

        Notifica n = NotificationFactory.createAcceptedNotification(booking);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToUser(booking.getClient().getId(), notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: rifiuto → notifica al CLR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingRejected(BookingRejectedEvent event) {
        Prenotazione booking = event.getBooking();

        if (booking.getClient() == null) return;  // CLG: nessuna notifica

        Notifica n = NotificationFactory.createRejectedNotification(booking);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToUser(booking.getClient().getId(), notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: annullamento da CLR → notifica al BAR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingCancelledByClient(BookingCancelledByClientEvent event) {
        Prenotazione booking = event.getBooking();
        User barber = getBarber();
        String reason = event.getReason();

        Notifica n = NotificationFactory.createClientCancellationNotification(booking, reason, barber);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToAllBarbers(notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // OBSERVER: cancellazione da BAR → notifica al CLR
    // -------------------------------------------------------
    @EventListener
    @Async
    public void onBookingCancelledByBarber(BookingCancelledByBarberEvent event) {
        Prenotazione booking = event.getBooking();

        if (booking.getClient() == null) return;  // CLG: nessuna notifica

        Notifica n = NotificationFactory.createBarberCancellationNotification(booking);
        n.setCreatedAt(LocalDateTime.now());
        notificaRepository.save(n);

        sseRegistry.pushToUser(booking.getClient().getId(), notificationMapper.toPushDto(n));
    }

    // -------------------------------------------------------
    // Recupero notifiche persistite
    // -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<NotificationResponseDto> getNotificationsForUser(Long userId) {
        return notificaRepository.findByDestinatarioIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(notificationMapper::toDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notifica n = notificaRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notifica non trovata: " + notificationId));
        if (!n.getDestinatario().getId().equals(userId)) {
            throw new UnauthorizedOperationException("Non autorizzato");
        }
        n.setLetta(true);
        notificaRepository.save(n);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificaRepository.markAllAsReadForUser(userId);
    }

    private User getBarber() {
        return userRepository.findByRuolo(UserRole.BARBER)
            .stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("Account BAR non trovato"));
    }
}
