CREATE TABLE schedules (
    id                BIGSERIAL PRIMARY KEY,
    chair_id          BIGINT NOT NULL REFERENCES chairs(id) ON DELETE CASCADE,
    giorno_settimana  VARCHAR(20) NOT NULL,   -- MONDAY, TUESDAY, ecc.
    ora_inizio        TIME NOT NULL,
    ora_fine          TIME NOT NULL,
    tipo              VARCHAR(20) NOT NULL,   -- APERTURA | PAUSA
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_schedule_hours CHECK (ora_inizio < ora_fine),
    CONSTRAINT chk_schedule_type  CHECK (tipo IN ('APERTURA', 'PAUSA'))
);

CREATE INDEX idx_schedule_chair_day ON schedules(chair_id, giorno_settimana);
CREATE INDEX idx_schedule_tipo      ON schedules(tipo);
