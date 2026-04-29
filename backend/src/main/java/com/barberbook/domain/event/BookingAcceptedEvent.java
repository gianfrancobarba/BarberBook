package com.barberbook.domain.event;

import com.barberbook.domain.model.Prenotazione;

public class BookingAcceptedEvent extends BookingEvent {
    public BookingAcceptedEvent(Object source, Prenotazione booking) {
        super(source, booking);
    }
}
