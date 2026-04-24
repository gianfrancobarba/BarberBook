CREATE TABLE services (
    id             BIGSERIAL PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL,
    descrizione    VARCHAR(500),
    durata_minuti  INTEGER NOT NULL CHECK (durata_minuti > 0),
    prezzo         NUMERIC(8, 2) NOT NULL CHECK (prezzo >= 0),
    attivo         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP
);

CREATE INDEX idx_services_attivo ON services(attivo);
