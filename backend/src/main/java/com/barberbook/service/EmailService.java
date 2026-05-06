package com.barberbook.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service per l'invio di email.
 * In fase di sviluppo logga semplicemente il contenuto.
 */
@Service
@Slf4j
public class EmailService {

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    /**
     * Invia l'email per il recupero della password.
     */
    public void sendPasswordResetEmail(String to, String nome, String token) {
        String resetLink = baseUrl + "/reset-password?token=" + token;
        // In sviluppo logghiamo il link per permettere il test senza un vero server SMTP
        log.info("📧 PASSWORD RESET EMAIL → Destinatario: {} ({}) | Link: {}", nome, to, resetLink);
    }
}
