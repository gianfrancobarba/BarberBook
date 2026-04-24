package com.barberbook.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configurazione Spring Security per Sprint 0.
 *
 * TEMPORANEA: permette tutte le richieste senza autenticazione.
 * Verrà sostituita completamente in Sprint 1 con:
 *   - JWT filter chain
 *   - RBAC (BARBER / CLIENT)
 *   - Protezione CSRF appropriata
 *
 * Attiva su tutti i profili (dev, test, prod) durante Sprint 0.
 * Il profilo 'test' userà questo stesso bean.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Sprint 0: CSRF disabilitato (API stateless — verrà gestito in Sprint 1)
            .csrf(AbstractHttpConfigurer::disable)
            // Sprint 0: tutto accessibile — sarà ristretto in Sprint 1
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
