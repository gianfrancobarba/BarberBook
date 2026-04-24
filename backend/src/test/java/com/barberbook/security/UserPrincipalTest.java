package com.barberbook.security;

import com.barberbook.domain.enums.UserRole;
import com.barberbook.domain.model.Barbiere;
import com.barberbook.domain.model.ClienteRegistrato;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    @Test
    @DisplayName("getAuthorities include il ruolo corretto")
    void getAuthorities_returnsRole() {
        ClienteRegistrato client = new ClienteRegistrato();
        UserPrincipal principal = new UserPrincipal(client);
        assertTrue(principal.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    @Test
    @DisplayName("getPassword ritorna hash per cliente")
    void getPassword_client_returnsHash() {
        ClienteRegistrato client = new ClienteRegistrato();
        client.setPasswordHash("hash123");
        UserPrincipal principal = new UserPrincipal(client);
        assertEquals("hash123", principal.getPassword());
    }

    @Test
    @DisplayName("getPassword ritorna stringa vuota per barbiere")
    void getPassword_barber_returnsEmpty() {
        Barbiere barber = new Barbiere();
        UserPrincipal principal = new UserPrincipal(barber);
        assertEquals("", principal.getPassword());
    }

    @Test
    @DisplayName("isAccountNonExpired e altri ritornano true")
    void checks_returnTrue() {
        UserPrincipal principal = new UserPrincipal(new Barbiere());
        assertTrue(principal.isAccountNonExpired());
        assertTrue(principal.isAccountNonLocked());
        assertTrue(principal.isCredentialsNonExpired());
        assertTrue(principal.isEnabled());
    }
}
