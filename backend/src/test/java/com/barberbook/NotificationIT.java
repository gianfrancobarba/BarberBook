package com.barberbook;

import com.barberbook.domain.event.BookingAcceptedEvent;
import com.barberbook.domain.model.*;
import com.barberbook.repository.NotificaRepository;
import com.barberbook.repository.UserRepository;
import com.barberbook.repository.PrenotazioneRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class NotificationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("barberbook_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLXRlc3RzLW9ubHk=");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private NotificaRepository notificaRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PrenotazioneRepository bookingRepository;
    @Autowired private ApplicationEventPublisher eventPublisher;

    private User client;
    private User barber;

    @BeforeEach
    void setUp() {
        notificaRepository.deleteAll();
        
        // Recupera Tony dal seed o crealo se non presente
        barber = userRepository.findByEmail("tony@hairmanbarber.it").orElseGet(() -> {
            Barbiere b = new Barbiere();
            b.setNome("Tony");
            b.setCognome("Barber");
            b.setEmail("tony@hairmanbarber.it");
            b.setCreatedAt(LocalDateTime.now());
            return userRepository.save(b);
        });

        client = userRepository.findByEmail("client@example.com").orElseGet(() -> {
            ClienteRegistrato c = new ClienteRegistrato();
            c.setNome("Mario");
            c.setCognome("Rossi");
            c.setEmail("client@example.com");
            c.setPasswordHash("hash");
            c.setCreatedAt(LocalDateTime.now());
            return userRepository.save(c);
        });
    }

    @Test
    @DisplayName("Evento BookingAccepted -> Notifica persistita su DB asincronamente")
    void bookingAccepted_persistsNotification() {
        // Given
        Prenotazione booking = createMockBooking(client);
        
        // When: Pubblichiamo l'evento manualmente per testare l'Observer
        eventPublisher.publishEvent(new BookingAcceptedEvent(this, booking));

        // Then: Verifichiamo con Awaitility (perché l'observer è @Async)
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            long count = notificaRepository.countByDestinatarioIdAndLettaFalse(client.getId());
            assertEquals(1, count);
        });
    }

    @Test
    @DisplayName("Guest Booking -> Nessuna notifica generata")
    void guestBooking_noNotification() {
        Prenotazione guestBooking = createMockBooking(null);
        guestBooking.setGuestData(new GuestData("Ospite", "Test", "123"));

        eventPublisher.publishEvent(new BookingAcceptedEvent(this, guestBooking));

        // Verifichiamo che dopo 2 secondi non ci sia ancora nulla (caso negativo)
        try { Thread.sleep(2000); } catch (InterruptedException e) {}
        assertEquals(0, notificaRepository.count());
    }

    @Test
    @DisplayName("API: Recupero notifiche e segna come letta")
    @WithMockUser(username = "client@example.com")
    void api_manageNotifications() throws Exception {
        // 1. Inseriamo una notifica manuale per il test API
        Notifica n = Notifica.builder()
                .destinatario(client)
                .tipo(com.barberbook.domain.enums.NotificationType.PRENOTAZIONE_ACCETTATA)
                .titolo("Test")
                .messaggio("Messaggio")
                .createdAt(LocalDateTime.now())
                .build();
        notificaRepository.save(n);

        // 2. GET /api/notifications
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].titolo").value("Test"));

        // 3. PATCH /{id}/read
        mockMvc.perform(patch("/api/notifications/" + n.getId() + "/read"))
                .andExpect(status().isOk());

        // 4. Verifica stato su DB
        assertTrue(notificaRepository.findById(n.getId()).get().isLetta());
    }

    private Prenotazione createMockBooking(User client) {
        // Per l'integrità del DB, dovremmo salvare anche servizio e poltrona, 
        // ma per questo test specifico dell'observer ci basta l'oggetto transient se non accediamo al DB via JPA cascata
        return Prenotazione.builder()
                .id(999L)
                .client(client)
                .startTime(LocalDateTime.now())
                .build();
    }
}
