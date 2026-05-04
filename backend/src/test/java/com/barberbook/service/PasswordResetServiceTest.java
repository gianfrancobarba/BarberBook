package com.barberbook.service;

import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.PasswordResetToken;
import com.barberbook.exception.InvalidTokenException;
import com.barberbook.repository.PasswordResetTokenRepository;
import com.barberbook.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @InjectMocks private PasswordResetService passwordResetService;

    @Test
    @DisplayName("requestPasswordReset: genera token e invia email per utente esistente")
    void requestReset_success() {
        ClienteRegistrato user = new ClienteRegistrato();
        user.setId(1L); user.setEmail("test@example.com"); user.setNome("Mario");
        
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        passwordResetService.requestPasswordReset("test@example.com");

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), eq("Mario"), anyString());
    }

    @Test
    @DisplayName("requestPasswordReset: non fa nulla se l'utente non esiste")
    void requestReset_userNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        passwordResetService.requestPasswordReset("unknown@example.com");

        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    @DisplayName("resetPassword: lancia eccezione se il token è scaduto")
    void resetPassword_expired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        token.setUsed(false);

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> 
            passwordResetService.resetPassword("raw-token", "new-pass"));
    }

    @Test
    @DisplayName("resetPassword: lancia eccezione se il token è già stato usato")
    void resetPassword_used() {
        PasswordResetToken token = new PasswordResetToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        token.setUsed(true);

        when(tokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class, () -> 
            passwordResetService.resetPassword("raw-token", "new-pass"));
    }
}
