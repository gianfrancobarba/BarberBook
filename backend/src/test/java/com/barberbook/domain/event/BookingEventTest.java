package com.barberbook.domain.event;

import com.barberbook.domain.model.Prenotazione;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingEventTest {

    @Test
    void testBookingAcceptedEvent() {
        Prenotazione p = Prenotazione.builder().id(1L).build();
        BookingAcceptedEvent event = new BookingAcceptedEvent(this, p);
        assertEquals(1L, event.getBooking().getId());
    }

    @Test
    void testBookingCancelledByBarberEvent() {
        Prenotazione p = Prenotazione.builder().id(1L).build();
        BookingCancelledByBarberEvent event = new BookingCancelledByBarberEvent(this, p);
        assertEquals(1L, event.getBooking().getId());
    }

    @Test
    void testBookingCancelledByClientEvent() {
        Prenotazione p = Prenotazione.builder().id(1L).build();
        BookingCancelledByClientEvent event = new BookingCancelledByClientEvent(this, p, "motivo");
        assertEquals(1L, event.getBooking().getId());
        assertEquals("motivo", event.getReason());
    }

    @Test
    void testBookingRejectedEvent() {
        Prenotazione p = Prenotazione.builder().id(1L).build();
        BookingRejectedEvent event = new BookingRejectedEvent(this, p);
        assertEquals(1L, event.getBooking().getId());
    }

    @Test
    void testBookingRequestCreatedEvent() {
        Prenotazione p = Prenotazione.builder().id(1L).build();
        BookingRequestCreatedEvent event = new BookingRequestCreatedEvent(this, p);
        assertEquals(1L, event.getBooking().getId());
    }
}
