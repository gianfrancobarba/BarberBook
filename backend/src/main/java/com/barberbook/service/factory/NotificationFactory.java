package com.barberbook.service.factory;

import com.barberbook.domain.enums.NotificationType;
import com.barberbook.domain.model.Notifica;
import com.barberbook.domain.model.Prenotazione;
import com.barberbook.domain.model.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Factory Method Pattern: ogni metodo statico crea una Notifica per uno specifico evento.
 * Classe non istanziabile — solo metodi factory statici.
 */
public final class NotificationFactory {

    private NotificationFactory() {}

    /** BAR: nuova richiesta di prenotazione ricevuta */
    public static Notifica createNewRequestNotification(Prenotazione booking, User barber) {
        return Notifica.builder()
            .destinatario(barber)
            .tipo(NotificationType.NUOVA_RICHIESTA)
            .titolo("Nuova richiesta di prenotazione")
            .messaggio(String.format(
                "%s ha richiesto '%s' per il %s alle %s sulla %s.",
                booking.getCustomerDisplayName(),
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime()),
                booking.getPoltrona().getNome()
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** CLR: la propria prenotazione è stata accettata */
    public static Notifica createAcceptedNotification(Prenotazione booking) {
        return Notifica.builder()
            .destinatario(booking.getClient())
            .tipo(NotificationType.PRENOTAZIONE_ACCETTATA)
            .titolo("Prenotazione confermata!")
            .messaggio(String.format(
                "La tua prenotazione per '%s' il %s alle %s è stata confermata.",
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime())
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** CLR: la propria prenotazione è stata rifiutata */
    public static Notifica createRejectedNotification(Prenotazione booking) {
        return Notifica.builder()
            .destinatario(booking.getClient())
            .tipo(NotificationType.PRENOTAZIONE_RIFIUTATA)
            .titolo("Prenotazione non confermata")
            .messaggio(String.format(
                "Purtroppo la tua richiesta per '%s' il %s alle %s non è stata confermata. " +
                "Puoi richiedere un altro orario.",
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime())
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** BAR: un CLR ha annullato la propria prenotazione */
    public static Notifica createClientCancellationNotification(Prenotazione booking,
                                                                  String reason,
                                                                  User barber) {
        return Notifica.builder()
            .destinatario(barber)
            .tipo(NotificationType.ANNULLAMENTO_DA_CLIENTE)
            .titolo("Prenotazione annullata dal cliente")
            .messaggio(String.format(
                "%s ha annullato la prenotazione del %s alle %s. Motivo: \"%s\"",
                booking.getCustomerDisplayName(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime()),
                reason
            ))
            .bookingId(booking.getId())
            .build();
    }

    /** CLR: il BAR ha cancellato una prenotazione confermata */
    public static Notifica createBarberCancellationNotification(Prenotazione booking) {
        return Notifica.builder()
            .destinatario(booking.getClient())
            .tipo(NotificationType.ANNULLAMENTO_DA_BARBIERE)
            .titolo("Prenotazione cancellata dal salone")
            .messaggio(String.format(
                "La tua prenotazione per '%s' del %s alle %s è stata cancellata dal salone. " +
                "Ci scusiamo per il disagio.",
                booking.getServizio().getNome(),
                formatDate(booking.getStartTime()),
                formatTime(booking.getStartTime())
            ))
            .bookingId(booking.getId())
            .build();
    }

    // --- Utility ---
    private static String formatDate(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String formatTime(LocalDateTime dt) {
        return dt.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
