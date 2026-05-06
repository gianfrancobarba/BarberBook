package com.barberbook.service;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.RefreshToken;
import com.barberbook.domain.model.User;
import com.barberbook.domain.model.PasswordResetToken;
import com.barberbook.dto.request.ForgotPasswordRequestDto;
import com.barberbook.dto.request.LoginRequestDto;
import com.barberbook.dto.request.RegisterRequestDto;
import com.barberbook.dto.request.ResetPasswordRequestDto;
import com.barberbook.dto.request.UpdateUserRequestDto;
import com.barberbook.dto.response.AuthResponseDto;
import com.barberbook.dto.response.UserResponseDto;
import com.barberbook.exception.EmailAlreadyExistsException;
import com.barberbook.exception.InvalidCredentialsException;
import com.barberbook.exception.InvalidTokenException;
import com.barberbook.exception.ResourceNotFoundException;
import com.barberbook.exception.TokenReuseDetectedException;
import com.barberbook.mapper.UserMapper;
import com.barberbook.repository.PasswordResetTokenRepository;
import com.barberbook.repository.RefreshTokenRepository;
import com.barberbook.repository.UserRepository;
import com.barberbook.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Value("${barber.password-hash:}")
    private String barberPasswordHash;

    @Value("${app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);
    private static final Duration RESET_TOKEN_EXPIRY = Duration.ofHours(1);

    public AuthResponseDto register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException("Email già registrata: " + dto.email());
        }
        ClienteRegistrato client = userMapper.toEntity(dto);
        client.setPasswordHash(passwordEncoder.encode(dto.password()));
        client.setCreatedAt(LocalDateTime.now());
        client.setEmailVerifiedAt(LocalDateTime.now());
        ClienteRegistrato saved = userRepository.save(client);
        return generateTokenPair(saved);
    }

    public AuthResponseDto login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.email())
            .orElseThrow(() -> new InvalidCredentialsException("Credenziali non valide"));

        String storedHash = extractPasswordHash(user);
        
        if (storedHash == null || storedHash.isEmpty()) {
             throw new IllegalStateException("Hash password non configurato per l'utente");
        }

        if (!passwordEncoder.matches(dto.password(), storedHash)) {
            throw new InvalidCredentialsException("Credenziali non valide");
        }

        return generateTokenPair(user);
    }

    public AuthResponseDto refresh(String refreshTokenRaw) {
        String tokenHash = hashToken(refreshTokenRaw);
        RefreshToken rt = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Refresh token non valido"));

        if (rt.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(rt.getUser().getId());
            throw new TokenReuseDetectedException("Sessione invalidata per sicurezza");
        }

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token scaduto");
        }

        rt.setRevoked(true);
        refreshTokenRepository.save(rt);
        return generateTokenPair(rt.getUser());
    }

    public UserResponseDto updateProfile(Long userId, UpdateUserRequestDto dto) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        if (dto.email() != null && !dto.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(dto.email())) {
                throw new EmailAlreadyExistsException("Email già in uso: " + dto.email());
            }
            user.setEmail(dto.email());
        }
        if (dto.nome() != null) user.setNome(dto.nome());
        if (dto.cognome() != null) user.setCognome(dto.cognome());
        if (dto.telefono() != null) user.setTelefono(dto.telefono());
        user.setUpdatedAt(LocalDateTime.now());

        return userMapper.toDto(userRepository.save(user));
    }

    public void requestPasswordReset(ForgotPasswordRequestDto dto) {
        userRepository.findByEmail(dto.email()).ifPresent(user -> {
            passwordResetTokenRepository.invalidateAllByUserId(user.getId());

            String rawToken = generateSecureToken();
            String tokenHash = hashToken(rawToken);

            PasswordResetToken prt = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plus(RESET_TOKEN_EXPIRY))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
            passwordResetTokenRepository.save(prt);

            String resetLink = appBaseUrl + "/reset-password?token=" + rawToken;
            log.info("Password reset link for {}: {}", dto.email(), resetLink);
        });
    }

    public void resetPassword(ResetPasswordRequestDto dto) {
        String tokenHash = hashToken(dto.token());
        PasswordResetToken prt = passwordResetTokenRepository.findByTokenHashAndUsedFalse(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Token non valido o già utilizzato"));

        if (prt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token scaduto");
        }

        User user = prt.getUser();
        if (user instanceof com.barberbook.domain.model.ClienteRegistrato client) {
            client.setPasswordHash(passwordEncoder.encode(dto.newPassword()));
            client.setUpdatedAt(LocalDateTime.now());
            userRepository.save(client);
        }

        prt.setUsed(true);
        passwordResetTokenRepository.save(prt);
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    public void logout(String refreshTokenRaw) {
        String tokenHash = hashToken(refreshTokenRaw);
        refreshTokenRepository.findByTokenHash(tokenHash)
            .ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
    }

    private AuthResponseDto generateTokenPair(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenRaw = generateSecureToken();
        String refreshTokenHash = hashToken(refreshTokenRaw);

        RefreshToken rt = RefreshToken.builder()
            .tokenHash(refreshTokenHash)
            .user(user)
            .expiresAt(LocalDateTime.now().plus(REFRESH_TOKEN_EXPIRY))
            .revoked(false)
            .createdAt(LocalDateTime.now())
            .build();
        refreshTokenRepository.save(rt);

        return new AuthResponseDto(accessToken, "Bearer", 900, refreshTokenRaw, userMapper.toDto(user));
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        return DigestUtils.sha256Hex(raw);
    }

    private String extractPasswordHash(User user) {
        String dbHash = user.getPasswordHash();
        if (dbHash != null) return dbHash;
        
        if (user.getRuolo() == UserRole.BARBER) {
            return barberPasswordHash;
        }
        return null;
    }
}
