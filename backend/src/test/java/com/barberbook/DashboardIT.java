package com.barberbook;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.*;
import com.barberbook.repository.UserRepository;
import com.barberbook.repository.PrenotazioneRepository;
import com.barberbook.repository.PoltronaRepository;
import com.barberbook.repository.ServizioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class DashboardIT {

    @Container
    @SuppressWarnings("resource")
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
    @Autowired private PrenotazioneRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PoltronaRepository chairRepository;
    @Autowired private ServizioRepository serviceRepository;

    private ClienteRegistrato client1;
    private ClienteRegistrato client2;
    private Poltrona chair;
    private Servizio service;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        chairRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        chair = new Poltrona(); chair.setNome("Poltrona 1"); chair.setAttiva(true);
        chair.setCreatedAt(LocalDateTime.now());
        chair = chairRepository.save(chair);

        service = new Servizio(); service.setNome("Taglio"); service.setPrezzo(java.math.BigDecimal.valueOf(20.0)); service.setDurataMinuti(30);
        service.setCreatedAt(LocalDateTime.now());
        service = serviceRepository.save(service);

        client1 = new ClienteRegistrato();
        client1.setNome("Mario"); client1.setCognome("Rossi"); client1.setEmail("mario@example.com");
        client1.setPasswordHash("hash"); client1.setCreatedAt(LocalDateTime.now());
        client1 = userRepository.save(client1);

        client2 = new ClienteRegistrato();
        client2.setNome("Luigi"); client2.setCognome("Verdi"); client2.setEmail("luigi@example.com");
        client2.setPasswordHash("hash"); client2.setCreatedAt(LocalDateTime.now());
        client2 = userRepository.save(client2);

        Barbiere barber = new Barbiere();
        barber.setNome("Tony"); barber.setCognome("Barber");
        barber.setEmail("tony@barber.it");
        barber.setCreatedAt(LocalDateTime.now());
        userRepository.save(barber);
    }

    @Test
    @DisplayName("Dashboard BAR: Accessibile solo ai barbieri")
    @WithUserDetails("tony@barber.it")
    void dailyDashboard_accessControl() throws Exception {
        mockMvc.perform(get("/api/dashboard/daily"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Dashboard BAR: Ritorna dati corretti per il giorno")
    @WithUserDetails("tony@barber.it")
    void dailyDashboard_dataValidation() throws Exception {
        LocalDate today = LocalDate.now();
        createBooking(client1, today.atTime(10, 0), BookingStatus.ACCETTATA);
        createBooking(client2, today.atTime(11, 0), BookingStatus.RIFIUTATA); // Non deve apparire

        mockMvc.perform(get("/api/dashboard/daily?date=" + today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today.toString()))
                .andExpect(jsonPath("$.chairs[0].bookings.length()").value(1))
                .andExpect(jsonPath("$.chairs[0].bookings[0].customerName").value("Mario Rossi"));
    }

    @Test
    @DisplayName("Dashboard BAR Settimanale: Ritorna sempre 7 giorni")
    @WithUserDetails("tony@barber.it")
    void weeklyDashboard_always7Days() throws Exception {
        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        
        mockMvc.perform(get("/api/dashboard/weekly?weekStart=" + monday))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days.length()").value(7))
                .andExpect(jsonPath("$.days[0].date").value(monday.toString()));
    }

    @Test
    @DisplayName("Isolamento Dati: Client 1 non vede prenotazioni di Client 2")
    @WithUserDetails("mario@example.com")
    void clientDataIsolation() throws Exception {
        LocalDate today = LocalDate.now();
        createBooking(client1, today.atTime(10, 0), BookingStatus.ACCETTATA);
        createBooking(client2, today.atTime(11, 0), BookingStatus.ACCETTATA);

        mockMvc.perform(get("/api/client/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerName").value("Mario Rossi"));
    }

    @Test
    @DisplayName("Homepage CLR: Mostra solo prossimi appuntamenti confermati")
    @WithUserDetails("mario@example.com")
    void clientHomepage_data() throws Exception {
        LocalDateTime future = LocalDateTime.now().plusDays(1);
        createBooking(client1, future, BookingStatus.ACCETTATA);
        createBooking(client1, future.plusHours(1), BookingStatus.IN_ATTESA); // Non confermata, non appare in upcoming

        mockMvc.perform(get("/api/client/homepage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.upcomingBookings.length()").value(1));
    }

    private void createBooking(User client, LocalDateTime start, BookingStatus status) {
        Prenotazione b = Prenotazione.builder()
                .client(client)
                .poltrona(chair)
                .servizio(service)
                .startTime(start)
                .endTime(start.plusMinutes(30))
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        bookingRepository.save(b);
    }
}
