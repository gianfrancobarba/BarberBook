package com.barberbook.domain.model;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AvailabilityContextTest {

    @Test
    void testConstructorAndGetters() {
        FasciaOraria schedule = new FasciaOraria();
        List<FasciaOraria> breaks = List.of(new FasciaOraria());
        List<Prenotazione> existingBookings = List.of(new Prenotazione());

        AvailabilityContext context = new AvailabilityContext(schedule, breaks, existingBookings);

        assertEquals(schedule, context.schedule());
        assertEquals(breaks, context.breaks());
        assertEquals(existingBookings, context.existingBookings());
    }
}
