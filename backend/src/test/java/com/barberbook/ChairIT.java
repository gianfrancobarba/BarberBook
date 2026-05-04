package com.barberbook;

import com.barberbook.dto.request.CreateChairRequestDto;
import com.barberbook.dto.request.LoginRequestDto;
import com.barberbook.dto.request.UpdateChairRequestDto;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@org.springframework.transaction.annotation.Transactional
class ChairIT {

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
    @DisplayName("GET /api/chairs -> ritorna le poltrone seedate (V7)")
    void getAll_public_returnsSeedChairs() throws Exception {
        mockMvc.perform(get("/api/chairs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nome").value("Poltrona 1"))
                .andExpect(jsonPath("$[1].nome").value("Poltrona 2"));
    }

    @Test
    @DisplayName("POST /api/chairs -> BARBER può aggiungere una poltrona")
    @WithUserDetails("tony@hairmanbarber.it")
    void create_asBarber_success() throws Exception {
        CreateChairRequestDto dto = new CreateChairRequestDto("Poltrona VIP");

        mockMvc.perform(post("/api/chairs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Poltrona VIP"));

        // Verifica incremento
        mockMvc.perform(get("/api/chairs"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("POST /api/chairs -> nome duplicato ritorna 409 Conflict")
    @WithUserDetails("tony@hairmanbarber.it")
    void create_duplicateName_returns409() throws Exception {
        CreateChairRequestDto dto = new CreateChairRequestDto("Poltrona 1");

        mockMvc.perform(post("/api/chairs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("PATCH /api/chairs/{id} -> BARBER può rinominare")
    @WithUserDetails("tony@hairmanbarber.it")
    void rename_asBarber_success() throws Exception {
        UpdateChairRequestDto dto = new UpdateChairRequestDto("Poltrona Master");

        mockMvc.perform(patch("/api/chairs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Poltrona Master"));
    }

    @Test
    @DisplayName("DELETE /api/chairs/{id} -> soft delete nasconde la poltrona")
    @WithUserDetails("tony@hairmanbarber.it")
    void delete_asBarber_softDeleteWorks() throws Exception {
        mockMvc.perform(delete("/api/chairs/2"))
                .andExpect(status().isNoContent());

        // Verifichiamo che sparisca dalla lista pubblica
        mockMvc.perform(get("/api/chairs"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Poltrona 1"));
    }

    @Test
    @DisplayName("POST /api/chairs -> GUEST riceve 401 Unauthorized")
    void create_noAuth_returns401() throws Exception {
        CreateChairRequestDto dto = new CreateChairRequestDto("Fail");

        mockMvc.perform(post("/api/chairs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/chairs -> CLIENT riceve 403 Forbidden")
    @WithMockUser(roles = "CLIENT")
    void create_asClient_returns403() throws Exception {
        mockMvc.perform(post("/api/chairs")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Vietata\"}"))
                .andExpect(status().isForbidden());
    }
}
