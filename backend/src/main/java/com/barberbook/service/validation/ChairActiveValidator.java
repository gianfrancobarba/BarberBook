package com.barberbook.service.validation;

import com.barberbook.exception.BookingValidationException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class ChairActiveValidator implements BookingValidator {
    @Override
    public void validate(BookingValidationRequest req) {
        if (!req.chair().isAttiva()) {
            throw new BookingValidationException("La poltrona selezionata non è disponibile.");
        }
    }
}
