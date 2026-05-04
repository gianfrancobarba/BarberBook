package com.barberbook.controller;

import com.barberbook.dto.response.NotificationResponseDto;
import com.barberbook.security.UserPrincipal;
import com.barberbook.service.NotificationService;
import com.barberbook.service.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Controller per la gestione delle notifiche in-app e dello streaming SSE.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterRegistry sseRegistry;
    private final NotificationService notificationService;

    /**
     * RF_GEN_5 — Apre una connessione SSE per ricevere push in real-time.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return sseRegistry.register(principal.getId());
    }

    /**
     * RF_BAR_16 / RF_CLR_7 — Lista notifiche persistite (storico)
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(
            notificationService.getNotificationsForUser(principal.getId()));
    }

    /**
     * Segna una notifica come letta.
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Segna tutte le notifiche come lette.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok().build();
    }
}
