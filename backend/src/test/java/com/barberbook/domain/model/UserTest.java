package com.barberbook.domain.model;

import com.barberbook.domain.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    @DisplayName("getRuolo ritorna BARBER per istanza Barbiere")
    void getRuolo_barberInstance_returnsBarber() {
        User user = new Barbiere();
        assertEquals(UserRole.BARBER, user.getRuolo());
    }

    @Test
    @DisplayName("getRuolo ritorna CLIENT per istanza ClienteRegistrato")
    void getRuolo_clientInstance_returnsClient() {
        User user = new ClienteRegistrato();
        assertEquals(UserRole.CLIENT, user.getRuolo());
    }

    @Test
    @DisplayName("getRuolo ritorna null per istanza User base")
    void getRuolo_baseInstance_returnsNull() {
        User user = new User() {
            @Override
            public Long getId() { return 1L; }
        };
        assertNull(user.getRuolo());
    }
}
