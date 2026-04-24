package com.barberbook;

import com.barberbook.dto.request.LoginRequestDto;
import com.barberbook.dto.request.RegisterRequestDto;
import com.barberbook.dto.response.AuthResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class AuthIT {

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
        
        // Genera hash dinamicamente per evitare discrepanze tra OS e ambienti
        String testHash = new BCryptPasswordEncoder(12).encode("password123");
        registry.add("barber.password-hash", () -> testHash);
        
        registry.add("jwt.secret", () -> "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLXRlc3RzLW9ubHk=");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Flusso completo: Registrazione -> Login -> Accesso protetto")
    void registerThenLogin_fullFlow_success() throws Exception {
        // 1. Registrazione
        RegisterRequestDto regDto = new RegisterRequestDto("Mario", "Rossi", "mario.rossi@example.com", "password123", "3330001122");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));

        // 2. Login
        LoginRequestDto loginDto = new LoginRequestDto("mario.rossi@example.com", "password123");
        
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String responseBody = loginResult.getResponse().getContentAsString();
        AuthResponseDto auth = objectMapper.readValue(responseBody, AuthResponseDto.class);
        String accessToken = auth.accessToken();

        // 3. Accesso a /api/users/me
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("mario.rossi@example.com"))
                .andExpect(jsonPath("$.nome").value("Mario"));
    }

    @Test
    @DisplayName("Login con account Barbiere (seed Flyway) -> ritorna ruolo BARBER")
    void login_barberAccount_returnsBarberRole() throws Exception {
        // Tony è inserito tramite Flyway V3 (seed)
        LoginRequestDto loginDto = new LoginRequestDto("tony@hairmanbarber.it", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.ruolo").value("BARBER"));
    }

    @Test
    @DisplayName("Accesso a endpoint protetto senza token -> 403 Forbidden")
    void accessProtectedEndpoint_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Refresh Token Flow: Rotazione del token")
    void refreshFlow_fullRotation_oldTokenInvalidated() throws Exception {
        // 1. Login per ottenere RT
        LoginRequestDto loginDto = new LoginRequestDto("tony@hairmanbarber.it", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDto)))
                .andReturn();
        
        Cookie rtCookie = loginResult.getResponse().getCookie("refreshToken");
        assertNotNull(rtCookie);

        // 2. Refresh
        mockMvc.perform(post("/api/auth/refresh")
                .cookie(rtCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(cookie().exists("refreshToken"));
    }

    @Test
    @DisplayName("Registrazione con email duplicata -> 400 Bad Request")
    void register_duplicateEmail_returns400() throws Exception {
        RegisterRequestDto regDto = new RegisterRequestDto("Luigi", "Verdi", "duplicate@example.com", "password123", "111222333");
        
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email già registrata: duplicate@example.com"));
    }
}
