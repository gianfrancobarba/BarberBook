CREATE TABLE chairs (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    attiva     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    CONSTRAINT uq_chair_nome UNIQUE (nome)
);

CREATE INDEX idx_chairs_attiva ON chairs(attiva);
