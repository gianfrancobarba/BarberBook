package com.barberbook.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitario puro di HealthController — nessuna dipendenza Spring/Docker.
 *
 * Verifica la logica del controller in isolamento totale.
 * Non usa @SpringBootTest → istantaneo, nessun contesto da avviare.
 */
class HealthControllerTest {

    private final HealthController controller = new HealthController();

    @Test
    @DisplayName("health() risponde con status 200")
    void health_returns200() {
        var response = controller.health();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("health() contiene campo 'status' uguale a 'UP'")
    void health_containsStatusUp() {
        Map<String, Object> body = controller.health().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("status")).isEqualTo("UP");
    }

    @Test
    @DisplayName("health() contiene campo 'app' uguale a 'BarberBook'")
    void health_containsAppName() {
        Map<String, Object> body = controller.health().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("app")).isEqualTo("BarberBook");
    }

    @Test
    @DisplayName("health() contiene campo 'timestamp' non nullo")
    void health_containsTimestamp() {
        Map<String, Object> body = controller.health().getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("timestamp")).isNotNull().isInstanceOf(String.class);
    }
}
