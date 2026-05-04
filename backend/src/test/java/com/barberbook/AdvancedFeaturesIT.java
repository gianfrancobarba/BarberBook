package com.barberbook;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.*;
import com.barberbook.repository.*;
import com.barberbook.dto.request.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AdvancedFeaturesIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLXRlc3RzLW9ubHk=");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PrenotazioneRepository bookingRepository;
    @Autowired private PoltronaRepository chairRepository;
    @Autowired private ServizioRepository serviceRepository;

    private ClienteRegistrato testUser;
    private Poltrona chair;
    private Servizio service;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        userRepository.deleteAll();
        chairRepository.deleteAll();
        serviceRepository.deleteAll();

        testUser = new ClienteRegistrato();
        testUser.setNome("Mario"); testUser.setCognome("Rossi");
        testUser.setEmail("mario@example.com"); testUser.setPasswordHash("hash");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        chair = new Poltrona(); chair.setNome("P1"); chair.setAttiva(true);
        chair = chairRepository.save(chair);

        service = new Servizio(); service.setNome("Taglio"); service.setDurataMinuti(30); service.setAttivo(true);
        service.setPrezzo(java.math.BigDecimal.valueOf(20));
        service = serviceRepository.save(service);
    }

    @Test
    @DisplayName("PATCH /api/users/me: Aggiorna correttamente il profilo")
    @WithMockUser(username = "mario@example.com", roles = "CLIENT")
    void updateProfile_success() throws Exception {
        UpdateProfileRequestDto dto = new UpdateProfileRequestDto("Luigi", "Verdi", null, "3339998888");

        mockMvc.perform(patch("/api/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Luigi"))
                .andExpect(jsonPath("$.telefono").value("3339998888"));

        ClienteRegistrato updated = (ClienteRegistrato) userRepository.findByEmail("mario@example.com").get();
        assertEquals("Luigi", updated.getNome());
    }

    @Test
    @DisplayName("POST /api/bookings/{id}/rebook: Crea nuova prenotazione da una passata")
    @WithMockUser(username = "mario@example.com", roles = "CLIENT")
    void rebook_success() throws Exception {
        // Crea una prenotazione passata
        Prenotazione past = Prenotazione.builder()
                .client(testUser).poltrona(chair).servizio(service)
                .startTime(LocalDateTime.now().minusDays(10))
                .endTime(LocalDateTime.now().minusDays(10).plusMinutes(30))
                .status(BookingStatus.ACCETTATA).createdAt(LocalDateTime.now())
                .build();
        past = bookingRepository.save(past);

        LocalDate nextWeek = LocalDate.now().plusWeeks(1);
        RebookRequestDto dto = new RebookRequestDto(nextWeek, LocalTime.of(10, 0));

        mockMvc.perform(post("/api/bookings/" + past.getId() + "/rebook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        List<Prenotazione> all = bookingRepository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("POST /api/auth/guest-register: Converte ospite in cliente registrato")
    void guestRegister_success() throws Exception {
        // 1. Crea una prenotazione ospite
        GuestData guest = new GuestData("Guest", "User", "123456789");
        Prenotazione booking = Prenotazione.builder()
                .guestData(guest).poltrona(chair).servizio(service)
                .startTime(LocalDateTime.now().plusDays(1))
                .endTime(LocalDateTime.now().plusDays(1).plusMinutes(30))
                .status(BookingStatus.IN_ATTESA).createdAt(LocalDateTime.now())
                .build();
        booking = bookingRepository.save(booking);

        // 2. Registrazione
        GuestRegisterRequestDto dto = new GuestRegisterRequestDto(booking.getId(), "guest@example.com", "password123");

        mockMvc.perform(post("/api/auth/guest-register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("guest@example.com"));

        // 3. Verifica nel DB
        Prenotazione updatedBooking = bookingRepository.findById(booking.getId()).get();
        assertNotNull(updatedBooking.getClient());
        assertNull(updatedBooking.getGuestData());
        assertEquals("Guest", updatedBooking.getClient().getNome());
    }
}
