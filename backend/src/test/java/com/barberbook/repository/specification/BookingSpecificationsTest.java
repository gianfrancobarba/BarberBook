package com.barberbook.repository.specification;

import com.barberbook.domain.enums.BookingStatus;
import com.barberbook.domain.model.Prenotazione;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class BookingSpecificationsTest {

    @Test
    @DisplayName("Le specifiche possono essere composte tramite .and()")
    void specifications_canBeComposedWithAnd() {
        Specification<Prenotazione> spec = BookingSpecifications.byClient(1L)
                .and(BookingSpecifications.byStatus(BookingStatus.ACCETTATA));
        
        assertNotNull(spec);
    }

    @Test
    @DisplayName("upcomingConfirmed è una composizione valida")
    void upcomingConfirmed_isValidComposition() {
        Specification<Prenotazione> spec = BookingSpecifications.upcomingConfirmed();
        assertNotNull(spec);
    }
}
