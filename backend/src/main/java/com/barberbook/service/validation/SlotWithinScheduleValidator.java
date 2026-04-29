package com.barberbook.service.validation;

import com.barberbook.exception.BookingValidationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Component
@Order(2)
public class SlotWithinScheduleValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        if (req.schedule() == null) {
            throw new BookingValidationException("Il salone è chiuso nel giorno selezionato.");
        }
        LocalTime startT = req.startTime().toLocalTime();
        LocalTime endT = req.endTime().toLocalTime();
        
        if (startT.isBefore(req.schedule().getOraInizio()) ||
            endT.isAfter(req.schedule().getOraFine())) {
            throw new BookingValidationException("Lo slot richiesto è fuori dagli orari di apertura.");
        }
    }
}
