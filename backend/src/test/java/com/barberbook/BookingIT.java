package com.barberbook;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.dto.request.BookingRequestDto;
import com.barberbook.dto.request.GuestBookingRequestDto;
import com.barberbook.dto.request.DirectBookingRequestDto;
import com.barberbook.repository.PrenotazioneRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Test
    @DisplayName("Flusso base: creazione richiesta ospite -> successo")
    void guestBookingFlow() throws Exception {
        var dto = new GuestBookingRequestDto(
            1L, 1L, LocalDate.now().plusDays(2), LocalTime.of(10, 0),
            "Mario", "Rossi", "3331234567"
        );

        mockMvc.perform(post("/api/bookings/guest")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "barber@test.com", roles = "BARBER")
    @DisplayName("BAR crea prenotazione diretta -> successo")
    void directBookingFlow() throws Exception {
        var dto = new DirectBookingRequestDto(
            2L, 2L, LocalDate.now().plusDays(3), LocalTime.of(11, 0),
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
            1L, 1L, LocalDate.now().plusDays(5), LocalTime.of(10, 0),
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

        long dbCount = prenotazioneRepository.findAll().stream()
            .filter(p -> p.getPoltrona().getId().equals(1L) && 
                         p.getStartTime().toLocalTime().equals(LocalTime.of(10, 0)) &&
                         p.getStatus() == BookingStatus.IN_ATTESA)
            .count();

        assertEquals(1, dbCount, "Dovrebbe esserci esattamente 1 prenotazione nel DB");
        assertEquals(1, successCount.get(), "Dovrebbe esserci esattamente 1 successo (201)");
    }
}
