package com.barberbook.service.validation;

import com.barberbook.exception.SlotNotAvailableException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class NoOverlapValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        boolean hasOverlap = req.existingBookings().stream()
            .filter(b -> req.excludeBookingId() == null || !b.getId().equals(req.excludeBookingId()))
            .anyMatch(b -> b.overlaps(req.startTime(), req.endTime()));

        if (hasOverlap) {
            throw new SlotNotAvailableException("Lo slot selezionato non è più disponibile.");
        }
    }
}
