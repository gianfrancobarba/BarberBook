CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token_hash  VARCHAR(255) NOT NULL UNIQUE,   -- SHA-256 del token — mai in chiaro
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP NOT NULL,             -- scadenza 1 ora
    used        BOOLEAN NOT NULL DEFAULT FALSE, -- token monouso
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prt_token  ON password_reset_tokens(token_hash);
CREATE INDEX idx_prt_user   ON password_reset_tokens(user_id);
CREATE INDEX idx_prt_expiry ON password_reset_tokens(expires_at);
