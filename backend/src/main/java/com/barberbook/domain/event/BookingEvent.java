package com.barberbook.domain.event;

import com.barberbook.domain.model.Prenotazione;
import org.springframework.context.ApplicationEvent;

/**
 * Evento base per le operazioni sulle prenotazioni.
 */
public abstract class BookingEvent extends ApplicationEvent {
    private final Prenotazione booking;

    protected BookingEvent(Object source, Prenotazione booking) {
        super(source);
        this.booking = booking;
    }

    public Prenotazione getBooking() {
        return booking;
    }
}
