package com.barberbook.domain.enums;

import com.barberbook.exception.InvalidBookingTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("Unit Test: State Machine (BookingStatus)")
class BookingStatusTest {

    @Test
    @DisplayName("Transizioni valide da IN_ATTESA")
    void validTransitionsFromInAttesa() {
        assertTrue(BookingStatus.IN_ATTESA.canTransitionTo(BookingStatus.ACCETTATA));
        assertTrue(BookingStatus.IN_ATTESA.canTransitionTo(BookingStatus.RIFIUTATA));
        assertTrue(BookingStatus.IN_ATTESA.canTransitionTo(BookingStatus.ANNULLATA));
    }

    @Test
    @DisplayName("Transizioni valide da ACCETTATA")
    void validTransitionsFromAccettata() {
        assertTrue(BookingStatus.ACCETTATA.canTransitionTo(BookingStatus.ANNULLATA));
        assertTrue(BookingStatus.ACCETTATA.canTransitionTo(BookingStatus.PASSATA));
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = {"RIFIUTATA", "ANNULLATA", "PASSATA"})
    @DisplayName("Stati terminali non dovrebbero avere transizioni")
    void terminalStatesHaveNoTransitions(BookingStatus status) {
        assertTrue(status.isTerminal());
        for (BookingStatus next : BookingStatus.values()) {
            assertFalse(status.canTransitionTo(next));
        }
    }

    @Test
    @DisplayName("Transizioni illegali dovrebbero lanciare eccezione")
    void illegalTransitionsThrowException() {
        assertThrows(InvalidBookingTransitionException.class, 
            () -> BookingStatus.RIFIUTATA.transitionTo(BookingStatus.ACCETTATA));
        assertThrows(InvalidBookingTransitionException.class, 
            () -> BookingStatus.PASSATA.transitionTo(BookingStatus.ANNULLATA));
        assertThrows(InvalidBookingTransitionException.class, 
            () -> BookingStatus.ACCETTATA.transitionTo(BookingStatus.IN_ATTESA));
    }

    @Test
    @DisplayName("Esecuzione transizione valida ritorna il nuovo stato")
    void transitionToReturnsNextState() {
        assertEquals(BookingStatus.ACCETTATA, 
            BookingStatus.IN_ATTESA.transitionTo(BookingStatus.ACCETTATA));
    }

    @Test
    @DisplayName("isTerminal ritorna correttamente per stati non terminali")
    void isTerminalForNonTerminalStates() {
        assertFalse(BookingStatus.IN_ATTESA.isTerminal());
        assertFalse(BookingStatus.ACCETTATA.isTerminal());
    }
}
