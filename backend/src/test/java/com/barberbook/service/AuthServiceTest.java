package com.barberbook.service;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.RefreshToken;
import com.barberbook.domain.model.User;
import com.barberbook.dto.request.LoginRequestDto;
import com.barberbook.dto.request.RegisterRequestDto;
import com.barberbook.dto.response.AuthResponseDto;
import com.barberbook.dto.response.UserResponseDto;
import com.barberbook.exception.EmailAlreadyExistsException;
import com.barberbook.exception.InvalidCredentialsException;
import com.barberbook.exception.InvalidTokenException;
import com.barberbook.exception.TokenReuseDetectedException;
import com.barberbook.mapper.UserMapper;
import com.barberbook.repository.RefreshTokenRepository;
import com.barberbook.repository.UserRepository;
import com.barberbook.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock UserMapper userMapper;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(authService, "barberPasswordHash", "$2a$12$hashedpassword");
    }

    // --- Registrazione ---
    @Test
    @DisplayName("Registrazione con dati validi -> crea utente e ritorna token")
    void register_validData_returnsTokenPair() {
        RegisterRequestDto dto = new RegisterRequestDto("Mario", "Rossi", "mario@example.com", "1234567890", "password123");
        ClienteRegistrato client = new ClienteRegistrato();
        client.setId(1L);
        client.setEmail("mario@example.com");
        client.setRuolo(UserRole.CLIENT);

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(userMapper.toEntity(dto)).thenReturn(client);
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed_password");
        when(userRepository.save(any(ClienteRegistrato.class))).thenReturn(client);
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(userMapper.toDto(any(User.class))).thenReturn(new UserResponseDto(1L, "Mario", "Rossi", "mario@example.com", "1234567890", UserRole.CLIENT));

        AuthResponseDto result = authService.register(dto);

        assertNotNull(result);
        assertEquals("access_token", result.accessToken());
        assertEquals("Bearer", result.tokenType());
        verify(userRepository).save(any(ClienteRegistrato.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Registrazione con email duplicata -> lancia EmailAlreadyExistsException")
    void register_duplicateEmail_throwsEmailAlreadyExistsException() {
        RegisterRequestDto dto = new RegisterRequestDto("Mario", "Rossi", "mario@example.com", "1234567890", "password123");
        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> authService.register(dto));
    }

    // --- Login ---
    @Test
    @DisplayName("Login con credenziali valide -> ritorna coppia di token")
    void login_validCredentials_returnsTokenPair() {
        LoginRequestDto dto = new LoginRequestDto("mario@example.com", "password123");
        ClienteRegistrato user = new ClienteRegistrato();
        user.setId(1L);
        user.setEmail("mario@example.com");
        user.setPasswordHash("hashed_password");
        user.setRuolo(UserRole.CLIENT);

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "hashed_password")).thenReturn(true);
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("access_token");
        when(userMapper.toDto(any(User.class))).thenReturn(new UserResponseDto(1L, "Mario", "Rossi", "mario@example.com", "1234567890", UserRole.CLIENT));

        AuthResponseDto result = authService.login(dto);

        assertNotNull(result);
        assertEquals("access_token", result.accessToken());
    }

    @Test
    @DisplayName("Login con password errata -> lancia InvalidCredentialsException")
    void login_wrongPassword_throwsInvalidCredentialsException() {
        LoginRequestDto dto = new LoginRequestDto("mario@example.com", "wrong_password");
        ClienteRegistrato user = new ClienteRegistrato();
        user.setPasswordHash("hashed_password");

        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(dto.password(), "hashed_password")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(dto));
    }

    @Test
    @DisplayName("Login con email sconosciuta -> lancia InvalidCredentialsException")
    void login_unknownEmail_throwsInvalidCredentialsException() {
        LoginRequestDto dto = new LoginRequestDto("unknown@example.com", "password123");
        when(userRepository.findByEmail(dto.email())).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(dto));
    }

    // --- Refresh Token ---
    @Test
    @DisplayName("Refresh con token valido -> ritorna nuova coppia e revoca il precedente")
    void refresh_validToken_returnsNewPairAndRevokesOld() {
        String rawToken = "raw_refresh_token";
        ClienteRegistrato user = new ClienteRegistrato();
        user.setId(1L);
        user.setRuolo(UserRole.CLIENT);
        
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setRevoked(false);
        rt.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));
        when(jwtUtil.generateAccessToken(any(User.class))).thenReturn("new_access_token");
        when(userMapper.toDto(any(User.class))).thenReturn(new UserResponseDto(1L, "Mario", "Rossi", "mario@example.com", "1234567890", UserRole.CLIENT));

        AuthResponseDto result = authService.refresh(rawToken);

        assertNotNull(result);
        assertTrue(rt.isRevoked());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("Refresh con token scaduto -> lancia InvalidTokenException")
    void refresh_expiredToken_throwsInvalidTokenException() {
        RefreshToken rt = new RefreshToken();
        rt.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));

        assertThrows(InvalidTokenException.class, () -> authService.refresh("raw_token"));
    }

    @Test
    @DisplayName("Refresh con token già revocato (attacco replay) -> invalida tutta la sessione")
    void refresh_revokedToken_revokesAllSessionAndThrows() {
        ClienteRegistrato user = new ClienteRegistrato();
        user.setId(1L);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));

        assertThrows(TokenReuseDetectedException.class, () -> authService.refresh("raw_token"));
        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    // --- Logout ---
    @Test
    @DisplayName("Logout con token valido -> revoca il token")
    void logout_validToken_revokesToken() {
        RefreshToken rt = new RefreshToken();
        rt.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(rt));

        authService.logout("raw_token");

        assertTrue(rt.isRevoked());
        verify(refreshTokenRepository).save(rt);
    }
}
