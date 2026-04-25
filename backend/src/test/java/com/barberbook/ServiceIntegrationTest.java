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

    // Helper per ottenere il token del barbiere
    private String getBarberToken() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto("barber@barberbook.it", "admin1234");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        AuthResponseDto auth = objectMapper.readValue(body, AuthResponseDto.class);
        return auth.accessToken();
    }

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
    void create_asBarber_success() throws Exception {
        String token = getBarberToken();
        CreateServiceRequestDto dto = new CreateServiceRequestDto("Colore", "Tinta capelli", 60, new BigDecimal("35.00"));

        mockMvc.perform(post("/api/services")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Colore"));
        
        // Verifica che ora siano 6
        mockMvc.perform(get("/api/services"))
                .andExpect(jsonPath("$.length()").value(6));
    }

    @Test
    @DisplayName("POST /api/services -> invalid duration returns 400 Bad Request")
    void create_asBarber_invalidDuration_returns400() throws Exception {
        String token = getBarberToken();
        CreateServiceRequestDto dto = new CreateServiceRequestDto("Nome", "Desc", 0, new BigDecimal("10.00"));

        mockMvc.perform(post("/api/services")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/services -> client riceve 403 Forbidden")
    void create_asClient_forbidden() throws Exception {
        // Registriamo un cliente per avere un token
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Test\",\"cognome\":\"User\",\"email\":\"client@test.it\",\"password\":\"password\",\"telefono\":\"123\"}"));
        
        String loginBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"client@test.it\",\"password\":\"password\"}"))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readValue(loginBody, AuthResponseDto.class).accessToken();

        CreateServiceRequestDto dto = new CreateServiceRequestDto("Vietato", "...", 30, BigDecimal.TEN);

        mockMvc.perform(post("/api/services")
                .header("Authorization", "Bearer " + token)
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
    @DisplayName("PATCH /api/services/{id} -> barbiere può modificare un servizio")
    void update_asBarber_success() throws Exception {
        String token = getBarberToken();
        UpdateServiceRequestDto dto = new UpdateServiceRequestDto("Nuovo Nome", null, null, null);

        mockMvc.perform(patch("/api/services/2")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nuovo Nome"));
    }

    @Test
    @DisplayName("DELETE /api/services/{id} -> soft delete funziona")
    void delete_asBarber_softDeleteWorks() throws Exception {
        String token = getBarberToken();
        
        // Eliminiamo il servizio con ID 1 (Capelli)
        mockMvc.perform(delete("/api/services/1")
                .header("Authorization", "Bearer " + token))
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
