package com.barberbook.domain.model;

import java.time.LocalTime;
import java.util.Objects;

/**
 * Value Object immutabile che rappresenta uno slot temporale disponibile.
 * Non è un'entità JPA — viene calcolato a runtime da AvailabilityStrategy.
 */
public record TimeSlot(LocalTime start, LocalTime end) {

    public TimeSlot {
        Objects.requireNonNull(start, "start non può essere null");
        Objects.requireNonNull(end, "end non può essere null");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start deve essere prima di end");
        }
    }

    /** Verifica se questo slot si sovrappone con un altro */
    public boolean overlapsWith(TimeSlot other) {
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    /** Verifica se questo slot si sovrappone con un intervallo start-end */
    public boolean overlapsWith(LocalTime otherStart, LocalTime otherEnd) {
        return this.start.isBefore(otherEnd) && otherStart.isBefore(this.end);
    }

    /** Verifica se questo slot è contenuto interamente nell'orario di apertura */
    public boolean fitsWithin(LocalTime openTime, LocalTime closeTime) {
        return !start.isBefore(openTime) && !end.isAfter(closeTime);
    }
}
