package com.barberbook;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Sprint 0 — Integration Test.
 *
 * Verifica:
 * 1. Il contesto Spring si avvia senza errori (Flyway, JPA, Security)
 * 2. GET /api/health risponde 200 con { "status": "UP" }
 *
 * Usa Testcontainers per un PostgreSQL reale in container Docker.
 * Nessun mock di database — validazione end-to-end dello stack.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Tag("integration")
class BarberBookApplicationTests {

    /** Container PostgreSQL condiviso per tutti i test di questa classe */
    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("barberbook_test")
            .withUsername("test")
            .withPassword("test");

    /**
     * Override delle proprietà datasource a runtime con i valori del container.
     * Questo pattern (@DynamicPropertySource) è la soluzione consigliata da Spring
     * per integrare Testcontainers senza configurazioni statiche.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Contesto Spring si avvia correttamente (Flyway + JPA + Security)")
    void contextLoads() {
        // Se arriviamo qui senza eccezioni, il contesto è partito correttamente.
        // Flyway ha eseguito V1__initial_schema.sql, JPA ha validato lo schema.
    }

    @Test
    @DisplayName("GET /api/health → 200 OK con status UP")
    void healthEndpoint_returns200WithStatusUp() throws Exception {
        mockMvc.perform(get("/api/health")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.app").value("BarberBook"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
