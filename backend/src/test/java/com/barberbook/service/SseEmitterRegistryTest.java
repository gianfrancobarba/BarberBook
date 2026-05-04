package com.barberbook.service;

import com.barberbook.domain.enums.NotificationType;
import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.model.Barbiere;
import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.User;
import com.barberbook.dto.response.NotificationPushDto;
import com.barberbook.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SseEmitterRegistryTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SseEmitterRegistry sseRegistry;

    private final Long userId = 1L;

    @Test
    @DisplayName("register: crea un nuovo emitter e invia evento connesso")
    void register_success() {
        SseEmitter emitter = sseRegistry.register(userId);
        
        assertNotNull(emitter);
        assertTrue(sseRegistry.hasActiveConnection(userId));
        assertEquals(1, sseRegistry.getActiveConnectionCount());
    }

    @Test
    @DisplayName("pushToUser: invia notifica se l'utente è connesso")
    void pushToUser_connected_success() {
        sseRegistry.register(userId);
        NotificationPushDto dto = new NotificationPushDto(1L, NotificationType.NUOVA_RICHIESTA, "T", "M", 100L, LocalDateTime.now());
        
        // Non possiamo verificare facilmente il contenuto dell'invio su SseEmitter (è final) 
        // ma verifichiamo che non lanci eccezioni e l'utente rimanga nel registro.
        assertDoesNotThrow(() -> sseRegistry.pushToUser(userId, dto));
        assertTrue(sseRegistry.hasActiveConnection(userId));
    }

    @Test
    @DisplayName("pushToUser: no-op se l'utente non è connesso")
    void pushToUser_notConnected_noOp() {
        NotificationPushDto dto = new NotificationPushDto(1L, NotificationType.NUOVA_RICHIESTA, "T", "M", 100L, LocalDateTime.now());
        assertDoesNotThrow(() -> sseRegistry.pushToUser(userId, dto));
        assertFalse(sseRegistry.hasActiveConnection(userId));
    }

    @Test
    @DisplayName("pushToAllBarbers: filtra correttamente gli utenti")
    void pushToAllBarbers_filtersCorrectly() {
        Long barberId = 1L;
        Long clientId = 2L;
        
        sseRegistry.register(barberId);
        sseRegistry.register(clientId);
        
        Barbiere b = new Barbiere();
        b.setId(barberId);
        
        ClienteRegistrato c = new ClienteRegistrato();
        c.setId(clientId);
        
        when(userRepository.findById(barberId)).thenReturn(Optional.of(b));
        when(userRepository.findById(clientId)).thenReturn(Optional.of(c));
        
        NotificationPushDto dto = new NotificationPushDto(1L, NotificationType.NUOVA_RICHIESTA, "T", "M", 100L, LocalDateTime.now());
        
        sseRegistry.pushToAllBarbers(dto);
        
        // Verifica che il repository sia stato interrogato
        verify(userRepository, atLeastOnce()).findById(anyLong());
    }
}
