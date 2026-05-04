-- Fase 6.2 — Schema per la gestione delle notifiche in-app
CREATE TABLE notifications (
    id            BIGSERIAL PRIMARY KEY,
    recipient_id  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tipo          VARCHAR(30) NOT NULL,
    titolo        VARCHAR(200) NOT NULL,
    messaggio     VARCHAR(1000) NOT NULL,
    letta         BOOLEAN NOT NULL DEFAULT FALSE,
    booking_id    BIGINT REFERENCES bookings(id) ON DELETE SET NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_notification_tipo CHECK (tipo IN (
        'NUOVA_RICHIESTA',
        'PRENOTAZIONE_ACCETTATA',
        'PRENOTAZIONE_RIFIUTATA',
        'ANNULLAMENTO_DA_CLIENTE',
        'ANNULLAMENTO_DA_BARBIERE'
    ))
);

CREATE INDEX idx_notification_recipient ON notifications(recipient_id);
CREATE INDEX idx_notification_read      ON notifications(letta);
CREATE INDEX idx_notification_created   ON notifications(created_at DESC);
