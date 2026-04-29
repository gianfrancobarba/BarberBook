package com.barberbook.service.validation;

import com.barberbook.exception.BookingValidationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@Order(3)
public class SlotNotInBreakValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        LocalTime slotStart = req.startTime().toLocalTime();
        LocalTime slotEnd = req.endTime().toLocalTime();

        boolean inBreak = req.breaks().stream().anyMatch(b -> 
            slotStart.isBefore(b.getOraFine()) && b.getOraInizio().isBefore(slotEnd)
        );

        if (inBreak) {
            throw new BookingValidationException("Lo slot richiesto cade in una pausa del salone.");
        }
    }
}
