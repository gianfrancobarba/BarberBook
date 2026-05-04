package com.barberbook.service.factory;

import com.barberbook.domain.enums.NotificationType;
import com.barberbook.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NotificationFactoryTest {

    private Prenotazione booking;
    private User client;
    private User barber;

    @BeforeEach
    void setUp() {
        ClienteRegistrato c = new ClienteRegistrato();
        c.setId(1L);
        c.setNome("Mario");
        c.setCognome("Rossi");
        client = c;

        Barbiere b = new Barbiere();
        b.setId(2L);
        b.setNome("Tony");
        b.setCognome("Barber");
        barber = b;
        
        Servizio servizio = Servizio.builder().nome("Taglio Capelli").build();
        Poltrona poltrona = Poltrona.builder().nome("Poltrona 1").build();
        
        booking = Prenotazione.builder()
            .id(100L)
            .client(client)
            .servizio(servizio)
            .poltrona(poltrona)
            .startTime(LocalDateTime.of(2026, 5, 20, 10, 0))
            .build();
    }

    @Test
    @DisplayName("createNewRequestNotification: destinatario è il barbiere e il testo è corretto")
    void createNewRequestNotification_success() {
        Notifica n = NotificationFactory.createNewRequestNotification(booking, barber);
        
        assertEquals(barber, n.getDestinatario());
        assertEquals(NotificationType.NUOVA_RICHIESTA, n.getTipo());
        assertTrue(n.getMessaggio().contains("Mario Rossi"));
        assertTrue(n.getMessaggio().contains("Taglio Capelli"));
        assertTrue(n.getMessaggio().contains("20/05/2026"));
        assertTrue(n.getMessaggio().contains("10:00"));
    }

    @Test
    @DisplayName("createAcceptedNotification: destinatario è il cliente")
    void createAcceptedNotification_success() {
        Notifica n = NotificationFactory.createAcceptedNotification(booking);
        
        assertEquals(client, n.getDestinatario());
        assertEquals(NotificationType.PRENOTAZIONE_ACCETTATA, n.getTipo());
        assertEquals("Prenotazione confermata!", n.getTitolo());
    }

    @Test
    @DisplayName("createRejectedNotification: contiene istruzioni per nuovo orario")
    void createRejectedNotification_success() {
        Notifica n = NotificationFactory.createRejectedNotification(booking);
        
        assertEquals(client, n.getDestinatario());
        assertEquals(NotificationType.PRENOTAZIONE_RIFIUTATA, n.getTipo());
        assertTrue(n.getMessaggio().contains("Puoi richiedere un altro orario"));
    }

    @Test
    @DisplayName("createClientCancellationNotification: contiene il motivo dell'annullamento")
    void createClientCancellationNotification_success() {
        String reason = "Imprevisto lavorativo";
        Notifica n = NotificationFactory.createClientCancellationNotification(booking, reason, barber);
        
        assertEquals(barber, n.getDestinatario());
        assertTrue(n.getMessaggio().contains(reason));
    }

    @Test
    @DisplayName("createBarberCancellationNotification: messaggio di scuse in italiano")
    void createBarberCancellationNotification_success() {
        Notifica n = NotificationFactory.createBarberCancellationNotification(booking);
        
        assertEquals(client, n.getDestinatario());
        assertTrue(n.getMessaggio().contains("Ci scusiamo per il disagio"));
    }
}
