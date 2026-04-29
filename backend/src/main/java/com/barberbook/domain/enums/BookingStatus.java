package com.barberbook.domain.enums;

import com.barberbook.exception.InvalidBookingTransitionException;
import java.util.Map;
import java.util.Set;

/**
 * Rappresenta il ciclo di vita di una prenotazione (Pattern State).
 */
public enum BookingStatus {
    IN_ATTESA,    // Richiesta dal cliente, in attesa di approvazione del barbiere
    ACCETTATA,    // Confermata dal barbiere (o creata direttamente da lui)
    RIFIUTATA,    // Rifiutata dal barbiere (stato terminale)
    ANNULLATA,    // Annullata dal cliente o dal barbiere (stato terminale)
    PASSATA;      // Servizio completato o orario trascorso (stato terminale)

    // Mappa delle transizioni valide
    private static final Map<BookingStatus, Set<BookingStatus>> VALID_TRANSITIONS = Map.of(
        IN_ATTESA, Set.of(ACCETTATA, RIFIUTATA, ANNULLATA), // ANNULLATA permessa se il cliente ci ripensa subito
        ACCETTATA, Set.of(ANNULLATA, PASSATA),
        RIFIUTATA, Set.of(),    // Terminale
        ANNULLATA, Set.of(),    // Terminale
        PASSATA,   Set.of()     // Terminale
    );

    /**
     * Verifica se la transizione verso 'next' è legale.
     */
    public boolean canTransitionTo(BookingStatus next) {
        return VALID_TRANSITIONS.get(this).contains(next);
    }

    /**
     * Esegue la transizione o lancia un'eccezione se illegale.
     */
    public BookingStatus transitionTo(BookingStatus next) {
        if (!canTransitionTo(next)) {
            throw new InvalidBookingTransitionException(
                String.format("Transizione non valida: %s -> %s", this, next)
            );
        }
        return next;
    }

    /**
     * Ritorna true se lo stato è finale.
     */
    public boolean isTerminal() {
        return VALID_TRANSITIONS.get(this).isEmpty();
    }
}
