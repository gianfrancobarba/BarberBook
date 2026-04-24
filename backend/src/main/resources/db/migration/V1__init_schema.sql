-- V1: Schema iniziale BarberBook
-- Tabella users (base, verrà estesa in Sprint 1)
CREATE TABLE IF NOT EXISTS users (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    cognome    VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    telefono   VARCHAR(20),
    ruolo      VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_ruolo ON users(ruolo);
