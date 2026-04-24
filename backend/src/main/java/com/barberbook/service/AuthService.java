package com.barberbook.service;

import com.barberbook.domain.model.Barbiere;
import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.RefreshToken;
import com.barberbook.domain.model.User;
import com.barberbook.dto.request.LoginRequestDto;
import com.barberbook.dto.request.RegisterRequestDto;
import com.barberbook.dto.response.AuthResponseDto;
import com.barberbook.exception.EmailAlreadyExistsException;
import com.barberbook.exception.InvalidCredentialsException;
import com.barberbook.exception.InvalidTokenException;
import com.barberbook.exception.TokenReuseDetectedException;
import com.barberbook.mapper.UserMapper;
import com.barberbook.repository.RefreshTokenRepository;
import com.barberbook.repository.UserRepository;
import com.barberbook.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    @Value("${barber.password-hash:}")
    private String barberPasswordHash;

    private static final Duration REFRESH_TOKEN_EXPIRY = Duration.ofDays(7);

    public AuthResponseDto register(RegisterRequestDto dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException("Email già registrata: " + dto.email());
        }
        ClienteRegistrato client = userMapper.toEntity(dto);
        client.setPasswordHash(passwordEncoder.encode(dto.password()));
        client.setCreatedAt(LocalDateTime.now());
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

        return new AuthResponseDto(accessToken, "Bearer", 900, refreshTokenRaw);
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
        if (user instanceof ClienteRegistrato client) return client.getPasswordHash();
        if (user instanceof Barbiere) {
            return barberPasswordHash;
        }
        throw new IllegalStateException("Tipo utente sconosciuto");
    }
}
