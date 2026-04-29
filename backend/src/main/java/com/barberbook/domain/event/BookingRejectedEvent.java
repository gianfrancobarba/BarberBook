package com.barberbook.domain.event;

import com.barberbook.domain.model.Prenotazione;

public class BookingRejectedEvent extends BookingEvent {
    public BookingRejectedEvent(Object source, Prenotazione booking) {
        super(source, booking);
    }
}
