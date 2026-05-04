package com.barberbook.controller;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.User;
import com.barberbook.dto.response.BookingResponseDto;
import com.barberbook.dto.response.ClientHomepageDto;
import com.barberbook.security.UserPrincipal;
import com.barberbook.service.ClientPortalService;
import com.barberbook.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller per l'area personale del cliente (CLR).
 */
@RestController
@RequestMapping("/api/client")
@PreAuthorize("hasRole('CLIENT')")
@RequiredArgsConstructor
public class ClientPortalController {

    private final ClientPortalService clientPortalService;
    private final NotificationService notificationService;

    /**
     * RF_CLR_1 — Homepage con prossimi appuntamenti confermati e badge notifiche.
     */
    @GetMapping("/homepage")
    public ResponseEntity<ClientHomepageDto> getHomepage(
            @AuthenticationPrincipal UserPrincipal principal) {
        User client = principal.getUser();
        List<BookingResponseDto> upcoming = clientPortalService.getUpcomingBookings(client);
        long unread = notificationService.countUnreadForUser(principal.getId());

        return ResponseEntity.ok(new ClientHomepageDto(
            client.getNome() + " " + client.getCognome(),
            upcoming,
            upcoming.size(),
            unread
        ));
    }

    /**
     * RF_CLR_2 — Storico completo delle prenotazioni dell'utente autenticato.
     */
    @GetMapping("/bookings")
    public ResponseEntity<List<BookingResponseDto>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            clientPortalService.getBookingHistory(principal.getUser()));
    }

    /**
     * RF_CLR_3 — Storico filtrato per stato (es. ?status=ACCETTATA).
     */
    @GetMapping("/bookings/filter")
    public ResponseEntity<List<BookingResponseDto>> getByStatus(
            @RequestParam(required = false) BookingStatus status,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            clientPortalService.getBookingsByStatus(principal.getUser(), status));
    }

    /**
     * RF_CLR_2 — Lista dei soli appuntamenti futuri.
     */
    @GetMapping("/bookings/upcoming")
    public ResponseEntity<List<BookingResponseDto>> getUpcoming(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            clientPortalService.getUpcomingBookings(principal.getUser()));
    }
}
