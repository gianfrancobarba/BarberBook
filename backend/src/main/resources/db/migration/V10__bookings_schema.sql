-- Fase 5.3: Schema per la gestione delle prenotazioni
CREATE TABLE bookings (
    id                  BIGSERIAL PRIMARY KEY,
    chair_id            BIGINT NOT NULL REFERENCES chairs(id),
    service_id          BIGINT NOT NULL REFERENCES services(id),
    client_id           BIGINT REFERENCES users(id),    -- null per CLG (ospiti)

    -- Dati ospite (CLG)
    guest_nome          VARCHAR(100),
    guest_cognome       VARCHAR(100),
    guest_telefono      VARCHAR(20),

    start_time          TIMESTAMP NOT NULL,
    end_time            TIMESTAMP NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'IN_ATTESA',

    cancellation_reason VARCHAR(1000),
    version             BIGINT NOT NULL DEFAULT 0,      -- Optimistic Locking
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP,

    CONSTRAINT chk_booking_times   CHECK (start_time < end_time),
    CONSTRAINT chk_booking_status  CHECK (status IN ('IN_ATTESA','ACCETTATA','RIFIUTATA','ANNULLATA','PASSATA')),
    CONSTRAINT chk_guest_or_client CHECK (
        (client_id IS NOT NULL AND guest_nome IS NULL)
     OR (client_id IS NULL AND guest_nome IS NOT NULL AND guest_cognome IS NOT NULL AND guest_telefono IS NOT NULL)
    )
);

CREATE INDEX idx_booking_chair_date ON bookings(chair_id, start_time);
CREATE INDEX idx_booking_client     ON bookings(client_id);
CREATE INDEX idx_booking_status     ON bookings(status);
