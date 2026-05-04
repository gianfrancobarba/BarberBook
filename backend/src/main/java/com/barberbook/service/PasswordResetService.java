package com.barberbook.service;

import com.barberbook.domain.model.ClienteRegistrato;
import com.barberbook.domain.model.PasswordResetToken;
import com.barberbook.domain.model.User;
import com.barberbook.exception.InvalidTokenException;
import com.barberbook.repository.PasswordResetTokenRepository;
import com.barberbook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Service per la gestione del ciclo di vita dei token di recupero password.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final Duration TOKEN_EXPIRY = Duration.ofHours(1);

    /**
     * Avvia la procedura di reset password se l'email appartiene a un cliente registrato.
     * SECURITY: La risposta è silente in caso di email non trovata per evitare enumeration.
     */
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email)
            .filter(user -> user instanceof ClienteRegistrato)
            .map(user -> (ClienteRegistrato) user)
            .ifPresent(user -> {
            // Invalida i token precedenti
            tokenRepository.invalidatePreviousTokensForUser(user.getId());

            // Genera e salva il nuovo token (hashato)
            String tokenRaw = generateSecureToken();
            String tokenHash = hashToken(tokenRaw);

            PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(LocalDateTime.now().plus(TOKEN_EXPIRY))
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();
            tokenRepository.save(token);

            // Invia l'email con il token raw (in chiaro solo nell'email)
            emailService.sendPasswordResetEmail(user.getEmail(), user.getNome(), tokenRaw);
        });
    }

    /**
     * Esegue il reset effettivo della password verificando la validità del token.
     */
    public void resetPassword(String tokenRaw, String newPassword) {
        String tokenHash = hashToken(tokenRaw);

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> new InvalidTokenException("Token non valido o già utilizzato"));

        if (token.isUsed()) {
            throw new InvalidTokenException("Token già utilizzato");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token scaduto. Richiedere un nuovo link.");
        }

        // Aggiorna la password dell'utente
        User user = token.getUser();
        if (user instanceof ClienteRegistrato client) {
            client.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(client);
        }

        // Segna il token come utilizzato (monouso)
        token.setUsed(true);
        tokenRepository.save(token);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String raw) {
        return DigestUtils.sha256Hex(raw);
    }
}
