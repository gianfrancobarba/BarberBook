-- ============================================================
-- V1 — Schema iniziale BarberBook
-- Sprint 0: migrazione placeholder per verificare che Flyway
-- si connetta correttamente al database PostgreSQL.
--
-- Crea la tabella 'app_metadata' come marcatore di versione.
-- Le tabelle di dominio verranno create a partire da V2 (Sprint 1).
-- ============================================================

CREATE TABLE app_metadata (
    key   VARCHAR(100) PRIMARY KEY,
    value VARCHAR(500) NOT NULL
);

INSERT INTO app_metadata (key, value) VALUES
    ('app_name',    'BarberBook'),
    ('version',     '0.1.0'),
    ('initialized', NOW()::TEXT);
