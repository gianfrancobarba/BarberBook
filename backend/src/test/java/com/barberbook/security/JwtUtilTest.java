package com.barberbook.security;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.model.ClienteRegistrato;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "dGhpcy1pcy1hLXZlcnktc2VjdXJlLWFuZC1sb25nLXNlY3JldC1rZXktZm9yLXRlc3RzLW9ubHk=";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
    }

    @Test
    @DisplayName("Generazione token -> ritorna una stringa JWT non vuota")
    void generateToken_validUser_returnsValidJwt() {
        ClienteRegistrato user = new ClienteRegistrato();
        user.setId(1L);
        user.setEmail("test@example.com");


        String token = jwtUtil.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Validazione token -> estrae correttamente ID e ruolo")
    void validateToken_validJwt_extractsCorrectUserId() {
        ClienteRegistrato user = new ClienteRegistrato();
        user.setId(123L);
        user.setEmail("test@example.com");


        String token = jwtUtil.generateAccessToken(user);
        Claims claims = jwtUtil.validateAndExtract(token);

        assertEquals("123", claims.getSubject());
        assertEquals("CLIENT", claims.get("role"));
        assertEquals("test@example.com", claims.get("email"));
    }

    @Test
    @DisplayName("Validazione token manomesso -> lancia JwtException")
    void validateToken_tamperedJwt_throwsJwtException() {
        ClienteRegistrato user = new ClienteRegistrato();
        user.setId(1L);


        String token = jwtUtil.generateAccessToken(user);
        String tamperedToken = token + "modified";

        assertThrows(JwtException.class, () -> jwtUtil.validateAndExtract(tamperedToken));
    }
}
