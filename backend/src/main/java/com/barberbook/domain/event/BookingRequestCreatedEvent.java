package com.barberbook.domain.event;

import com.barberbook.domain.model.Prenotazione;

public class BookingRequestCreatedEvent extends BookingEvent {
    public BookingRequestCreatedEvent(Object source, Prenotazione booking) {
        super(source, booking);
    }
}
