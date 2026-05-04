package com.barberbook;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.dto.request.GuestBookingRequestDto;
import com.barberbook.dto.request.DirectBookingRequestDto;
import com.barberbook.domain.model.*;
import com.barberbook.repository.*;
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
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Transactional
public class BookingIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLXRlc3RzLW9ubHk=");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PrenotazioneRepository bookingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PoltronaRepository chairRepository;
    @Autowired private ServizioRepository serviceRepository;

    private Poltrona chair1;
    private Poltrona chair2;
    private Servizio service1;
    private Servizio service2;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        chairRepository.deleteAll();
        serviceRepository.deleteAll();
        userRepository.deleteAll();

        chair1 = new Poltrona(); chair1.setNome("P1"); chair1.setAttiva(true); chair1.setCreatedAt(LocalDateTime.now());
        chair1 = chairRepository.save(chair1);
        chair2 = new Poltrona(); chair2.setNome("P2"); chair2.setAttiva(true); chair2.setCreatedAt(LocalDateTime.now());
        chair2 = chairRepository.save(chair2);

        service1 = new Servizio(); service1.setNome("S1"); service1.setDurataMinuti(30); service1.setAttivo(true);
        service1.setPrezzo(java.math.BigDecimal.valueOf(20)); service1.setCreatedAt(LocalDateTime.now());
        service1 = serviceRepository.save(service1);
        service2 = new Servizio(); service2.setNome("S2"); service2.setDurataMinuti(30); service2.setAttivo(true);
        service2.setPrezzo(java.math.BigDecimal.valueOf(25)); service2.setCreatedAt(LocalDateTime.now());
        service2 = serviceRepository.save(service2);

        Barbiere b = new Barbiere();
        b.setNome("Tony"); b.setCognome("Barber"); b.setEmail("barber@test.com"); b.setCreatedAt(LocalDateTime.now());
        userRepository.save(b);
    }

    @Test
    @DisplayName("Flusso base: creazione richiesta ospite -> successo")
    void guestBookingFlow() throws Exception {
        var dto = new GuestBookingRequestDto(
            chair1.getId(), service1.getId(), LocalDate.now().plusDays(2), LocalTime.of(10, 0),
            "Mario", "Rossi", "3331234567"
        );

        mockMvc.perform(post("/api/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("barber@test.com")
    @DisplayName("BAR crea prenotazione diretta -> successo")
    void directBookingFlow() throws Exception {
        var dto = new DirectBookingRequestDto(
            chair2.getId(), service2.getId(), LocalDate.now().plusDays(3), LocalTime.of(11, 0),
            "Luigi", "Verdi", null
        );

        mockMvc.perform(post("/api/bookings/direct")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("CRITICO: 50 tentativi concorrenti sullo stesso slot -> esattamente 1 salvato")
    void concurrentBookings_preventDoubleBooking() throws Exception {
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        
        var dto = new GuestBookingRequestDto(
            chair1.getId(), service1.getId(), LocalDate.now().plusDays(5), LocalTime.of(10, 0),
            "Concorrente", "Test", "000000000"
        );
        String json = objectMapper.writeValueAsString(dto);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    var result = mockMvc.perform(post("/api/bookings/guest")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                            .andReturn();
                    
                    if (result.getResponse().getStatus() == 201) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        latch.countDown(); // Via!
        doneLatch.await();
        executor.shutdown();

        long dbCount = bookingRepository.findAll().stream()
            .filter(p -> p.getPoltrona().getId().equals(chair1.getId()) && 
                         p.getStartTime().toLocalTime().equals(LocalTime.of(10, 0)) &&
                         p.getStatus() == BookingStatus.IN_ATTESA)
            .count();

        assertEquals(1, dbCount, "Dovrebbe esserci esattamente 1 prenotazione nel DB");
        assertEquals(1, successCount.get(), "Dovrebbe esserci esattamente 1 successo (201)");
    }
}
