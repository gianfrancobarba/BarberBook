package com.barberbook.service.validation;

import com.barberbook.domain.model.FasciaOraria;
import com.barberbook.domain.model.Poltrona;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.Servizio;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingValidationRequestTest {

    @Test
    void testConstructorAndGetters() {
        Poltrona chair = new Poltrona();
        Servizio service = new Servizio();
        FasciaOraria schedule = new FasciaOraria();
        List<FasciaOraria> breaks = List.of(new FasciaOraria());
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusHours(1);
        List<Prenotazione> existing = List.of(new Prenotazione());

        BookingValidationRequest req = new BookingValidationRequest(
                chair, service, schedule, breaks, start, end, existing, 1L);

        assertEquals(chair, req.chair());
        assertEquals(service, req.service());
        assertEquals(schedule, req.schedule());
        assertEquals(breaks, req.breaks());
        assertEquals(start, req.startTime());
        assertEquals(end, req.endTime());
        assertEquals(existing, req.existingBookings());
        assertEquals(1L, req.excludeBookingId());
    }
}
