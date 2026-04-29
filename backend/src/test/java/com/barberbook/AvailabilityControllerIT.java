package com.barberbook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AvailabilityControllerIT {

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

    @Test
    @DisplayName("GET /api/availability -> ritorna slot corretti per un lunedì (seed V9)")
    void getAvailableSlots_standardMonday_success() throws Exception {
        // Lunedì 2026-05-04 (Lunedì)
        // Orario: 09:00 - 19:00, Pausa 13:00 - 15:00
        // Servizio 'Capelli' (id=1) -> 30 minuti
        
        mockMvc.perform(get("/api/availability")
                .param("date", "2026-05-04")
                .param("serviceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)) // Poltrona 1 e Poltrona 2
                .andExpect(jsonPath("$[0].chairName").value("Poltrona 1"))
                .andExpect(jsonPath("$[0].availableSlots.length()").value(30)); // 15 mattina + 15 pomeriggio
    }

    @Test
    @DisplayName("GET /api/availability -> domenica ritorna liste vuote (chiuso)")
    void getAvailableSlots_sunday_returnsEmpty() throws Exception {
        // Domenica 2026-05-10
        mockMvc.perform(get("/api/availability")
                .param("date", "2026-05-10")
                .param("serviceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].availableSlots").isEmpty())
                .andExpect(jsonPath("$[1].availableSlots").isEmpty());
    }

    @Test
    @DisplayName("GET /api/availability -> servizio inesistente ritorna 404")
    void getAvailableSlots_serviceNotFound_returns404() throws Exception {
        mockMvc.perform(get("/api/availability")
                .param("date", "2026-05-04")
                .param("serviceId", "999"))
                .andExpect(status().isNotFound());
    }
}
