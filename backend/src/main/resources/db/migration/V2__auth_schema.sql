-- Tabella barbers (specializzazione di users)
CREATE TABLE barbers (
    id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE
);

-- Tabella clients (specializzazione di users)
CREATE TABLE clients (
    id             BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    password_hash  VARCHAR(255) NOT NULL,
    email_verified_at TIMESTAMP
);

-- Tabella refresh_tokens
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);
