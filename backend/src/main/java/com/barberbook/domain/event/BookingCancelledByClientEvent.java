package com.barberbook.domain.event;

import com.barberbook.domain.model.Prenotazione;

public class BookingCancelledByClientEvent extends BookingEvent {
    private final String reason;

    public BookingCancelledByClientEvent(Object source, Prenotazione booking, String reason) {
        super(source, booking);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
