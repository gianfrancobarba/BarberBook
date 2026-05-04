package com.barberbook.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PrenotazioneTest {

    @Test
    void testOverlaps() {
        LocalDateTime start = LocalDateTime.of(2023, 10, 10, 10, 0);
        LocalDateTime end = LocalDateTime.of(2023, 10, 10, 11, 0);
        
        Prenotazione p = new Prenotazione();
        p.setStartTime(start);
        p.setEndTime(end);

        assertTrue(p.overlaps(start.plusMinutes(15), end.plusMinutes(15)));
        assertFalse(p.overlaps(start.minusHours(2), start.minusHours(1)));
    }

    @Test
    void testGetCustomerDisplayName() {
        Prenotazione p = new Prenotazione();
        assertEquals("Cliente sconosciuto", p.getCustomerDisplayName());

        ClienteRegistrato client = new ClienteRegistrato();
        client.setNome("Mario");
        client.setCognome("Rossi");
        p.setClient(client);
        assertEquals("Mario Rossi", p.getCustomerDisplayName());

        p.setClient(null);
        GuestData guest = new GuestData();
        guest.setNome("Luigi");
        guest.setCognome("Verdi");
        p.setGuestData(guest);
        assertEquals("Luigi Verdi (ospite)", p.getCustomerDisplayName());
    }
}
