package com.barberbook.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Endpoint di health check — verifica che lo stack sia avviato correttamente.
 *
 * GET /api/health
 * Risposta: { "status": "UP", "timestamp": "...", "app": "BarberBook" }
 *
 * Accessibile senza autenticazione (configurato in SecurityConfig Sprint 1).
 * Per Sprint 0 la security è disabilitata temporaneamente.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "BarberBook",
                "timestamp", LocalDateTime.now().toString()
        ));
    }
}
