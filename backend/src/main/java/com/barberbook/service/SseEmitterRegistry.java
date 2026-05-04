package com.barberbook.service;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.dto.response.NotificationPushDto;
import com.barberbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro centralizzato delle connessioni SSE attive.
 * Usa ConcurrentHashMap per thread-safety sotto carico concorrente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SseEmitterRegistry {

    private final UserRepository userRepository;

    // userId → SseEmitter attivo (un emitter per utente)
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Registra un nuovo emitter SSE per l'utente.
     * Se esisteva già una connessione precedente, viene sostituita.
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);  // nessun timeout

        emitters.put(userId, emitter);

        // Cleanup automatico alla chiusura della connessione
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        // Invia evento di connessione (heartbeat iniziale)
        try {
            emitter.send(SseEmitter.event().name("connected").data("Connessione SSE attiva"));
        } catch (IOException e) {
            log.error("Errore nell'invio dell'evento di connessione SSE per l'utente {}", userId, e);
            emitters.remove(userId);
        }

        return emitter;
    }

    /**
     * Invia una notifica a un utente specifico tramite SSE.
     * Se l'utente non ha una connessione attiva, non succede nulla (notifica persiste in DB).
     */
    public void pushToUser(Long userId, NotificationPushDto notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(notification));
            } catch (IOException e) {
                log.warn("Errore nell'invio della notifica SSE all'utente {}. Rimozione emitter.", userId);
                emitters.remove(userId);
            }
        }
    }

    /**
     * Invia una notifica a tutti gli utenti con ruolo BARBER connessi.
     * (Nel sistema attuale c'è un solo BAR, ma il codice è scalabile.)
     */
    public void pushToAllBarbers(NotificationPushDto notification) {
        emitters.keySet().forEach(userId -> {
            if (isBarber(userId)) {
                pushToUser(userId, notification);
            }
        });
    }

    private boolean isBarber(Long userId) {
        return userRepository.findById(userId)
            .map(u -> u.getRuolo() == UserRole.BARBER)
            .orElse(false);
    }

    /** Per i test: verifica se un utente ha una connessione attiva */
    public boolean hasActiveConnection(Long userId) {
        return emitters.containsKey(userId);
    }

    /** Per i test: numero connessioni attive */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
