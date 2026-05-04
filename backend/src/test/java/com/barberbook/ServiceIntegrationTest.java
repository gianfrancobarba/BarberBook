package com.barberbook;

import com.barberbook.dto.request.CreateServiceRequestDto;
import com.barberbook.dto.request.LoginRequestDto;
import com.barberbook.dto.request.UpdateServiceRequestDto;
import com.barberbook.dto.response.AuthResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@org.springframework.transaction.annotation.Transactional
class ServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("barber.password-hash", () -> new BCryptPasswordEncoder(12).encode("admin1234"));
        registry.add("jwt.secret", () -> "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLXRlc3RzLW9ubHk=");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @Test
    @DisplayName("GET /api/services -> ritorna i servizi seedati da Flyway")
    void getAll_public_returnsSeedServices() throws Exception {
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].nome").value("Capelli"));
    }

    @Test
    @DisplayName("POST /api/services -> barbiere può creare un servizio")
    @WithUserDetails("tony@hairmanbarber.it")
    void create_asBarber_success() throws Exception {
        CreateServiceRequestDto dto = new CreateServiceRequestDto(
            "Nuovo Taglio", "Taglio moderno", 45, new BigDecimal("25.00")
        );

        mockMvc.perform(post("/api/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Nuovo Taglio"));
        
        // Verifica che ora siano 6
        mockMvc.perform(get("/api/services"))
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("POST /api/services -> invalid duration returns 400 Bad Request")
    @WithUserDetails("tony@hairmanbarber.it")
    void create_asBarber_invalidDuration_returns400() throws Exception {
        CreateServiceRequestDto dto = new CreateServiceRequestDto("Nome", "Desc", 0, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/services -> cliente riceve 403 Forbidden")
    @WithMockUser(roles = "CLIENT")
    void create_asClient_returns403() throws Exception {
        CreateServiceRequestDto dto = new CreateServiceRequestDto(
            "Vietato", "", 10, new BigDecimal("10.00")
        );

        mockMvc.perform(post("/api/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/services/{id} -> ritorna 200 se esiste")
    void getById_existing_returns200() throws Exception {
        mockMvc.perform(get("/api/services/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Capelli"));
    }

    @Test
    @DisplayName("GET /api/services/{id} -> ritorna 404 se non esiste")
    void getById_notExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/services/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/services/{id} -> barbiere può aggiornare")
    @WithUserDetails("tony@hairmanbarber.it")
    void update_asBarber_success() throws Exception {
        UpdateServiceRequestDto dto = new UpdateServiceRequestDto(
            "Capelli Premium", null, null, new BigDecimal("25.00")
        );

        mockMvc.perform(patch("/api/services/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Capelli Premium"))
                .andExpect(jsonPath("$.prezzo").value(25.00));
    }

    @Test
    @DisplayName("DELETE /api/services/{id} -> soft delete funziona")
    @WithUserDetails("tony@hairmanbarber.it")
    void delete_asBarber_softDeleteWorks() throws Exception {
        
        // Eliminiamo il servizio con ID 1 (Capelli)
        mockMvc.perform(delete("/api/services/1"))
                .andExpect(status().isNoContent());

        // Verifichiamo che non appaia più nella lista pubblica
        mockMvc.perform(get("/api/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[?(@.id == 1)]").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/services -> senza token ritorna 401 Unauthorized")
    void create_noAuth_returns401() throws Exception {
        CreateServiceRequestDto dto = new CreateServiceRequestDto("Vietato", "...", 30, BigDecimal.TEN);

        mockMvc.perform(post("/api/services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }
}
