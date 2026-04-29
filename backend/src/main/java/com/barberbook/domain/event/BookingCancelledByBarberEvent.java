package com.barberbook.domain.event;

import com.barberbook.domain.model.Prenotazione;

public class BookingCancelledByBarberEvent extends BookingEvent {
    public BookingCancelledByBarberEvent(Object source, Prenotazione booking) {
        super(source, booking);
    }
}
