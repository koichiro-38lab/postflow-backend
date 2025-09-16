-- Refresh Tokens for JWT rotation and revocation
-- Stores only SHA-256 hex hash of the refresh token (never the raw value)

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL, -- SHA-256 hex (64 chars)
    issued_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    user_agent TEXT,
    ip_address INET,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_refresh_token_hash_hex CHECK (token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_refresh_expires_after_issue CHECK (expires_at > issued_at)
);

-- Helpful indexes for common queries and cleanup
CREATE INDEX idx_refresh_tokens_user_id     ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at  ON refresh_tokens(expires_at);
CREATE INDEX idx_refresh_tokens_active_user ON refresh_tokens(user_id)
  WHERE revoked_at IS NULL;

