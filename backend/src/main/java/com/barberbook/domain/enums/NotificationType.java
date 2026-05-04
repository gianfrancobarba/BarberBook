package com.barberbook.domain.enums;

/**
 * Tipi di notifiche supportate dal sistema.
 */
public enum NotificationType {
    NUOVA_RICHIESTA,           // → BAR: cliente ha inviato richiesta
    PRENOTAZIONE_ACCETTATA,    // → CLR: BAR ha accettato
    PRENOTAZIONE_RIFIUTATA,    // → CLR: BAR ha rifiutato
    ANNULLAMENTO_DA_CLIENTE,   // → BAR: CLR ha annullato con motivazione
    ANNULLAMENTO_DA_BARBIERE   // → CLR: BAR ha cancellato la prenotazione
}
