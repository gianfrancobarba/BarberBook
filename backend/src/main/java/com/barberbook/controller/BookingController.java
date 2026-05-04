package com.barberbook.controller;

import com.barberbook.dto.request.*;
import com.barberbook.dto.response.BookingResponseDto;
import com.barberbook.security.UserPrincipal;
import com.barberbook.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /** RF_CLI_6 — CLR invia richiesta */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<BookingResponseDto> createRequest(
            @Valid @RequestBody BookingRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(
            bookingService.createRequest(dto, principal.getUser()));
    }

    /** RF_CLG_1 — CLG invia richiesta senza account */
    @PostMapping("/guest")
    public ResponseEntity<BookingResponseDto> createGuestRequest(
            @Valid @RequestBody GuestBookingRequestDto dto) {
        return ResponseEntity.status(201).body(bookingService.createGuestRequest(dto));
    }

    /** RF_BAR_14 — BAR accetta */
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> accept(@PathVariable Long id) {
        bookingService.acceptRequest(id);
        return ResponseEntity.ok().build();
    }

    /** RF_BAR_15 — BAR rifiuta */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> reject(@PathVariable Long id) {
        bookingService.rejectRequest(id);
        return ResponseEntity.ok().build();
    }

    /** RF_BAR_11 — BAR crea prenotazione diretta */
    @PostMapping("/direct")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<BookingResponseDto> createDirect(
            @Valid @RequestBody DirectBookingRequestDto dto) {
        return ResponseEntity.status(201).body(bookingService.createDirect(dto));
    }

    /** RF_BAR_12 — BAR modifica */
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<BookingResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingRequestDto dto) {
        return ResponseEntity.ok(bookingService.update(id, dto));
    }

    /** RF_BAR_13 — BAR cancella */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<Void> cancelByBarber(@PathVariable Long id) {
        bookingService.cancelByBarber(id);
        return ResponseEntity.noContent().build();
    }

    /** RF_CLR_4 — CLR annulla con motivazione */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<Void> cancelByClient(
            @PathVariable Long id,
            @Valid @RequestBody CancelBookingRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        bookingService.cancelByClient(id, dto.reason(), principal.getUser());
        return ResponseEntity.ok().build();
    }

    /** RF_BAR_16 (preparatorio) — lista richieste in attesa */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('BARBER')")
    public ResponseEntity<List<BookingResponseDto>> getPending() {
        return ResponseEntity.ok(bookingService.getPendingRequests());
    }

    /** Storico prenotazioni CLR */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(bookingService.getClientBookings(principal.getUser()));
    }

    /** RF_CLR_5 — Riprenotazione rapida */
    @PostMapping("/{id}/rebook")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<BookingResponseDto> rebook(
            @PathVariable Long id,
            @Valid @RequestBody RebookRequestDto dto,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(201).body(
            bookingService.rebook(id, dto.date(), dto.startTime(), principal.getUser()));
    }
}
