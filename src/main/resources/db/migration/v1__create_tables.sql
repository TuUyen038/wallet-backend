CREATE TABLE users
(
    id         BIGSERIAL PRIMARY KEY,
    email      TEXT NOT NULL CHECK (email <> ''),
    password   TEXT NOT NULL,
    full_name  TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_users_email_lower
ON users (LOWER(email));


CREATE TABLE refresh_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT NOT NULL UNIQUE,
    expired_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked);

-- partial index (pro)
CREATE INDEX idx_refresh_tokens_active
ON refresh_tokens (user_id)
WHERE revoked = false;


-- Trigger
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_set_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();


CREATE TABLE wallets
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance    BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TYPE transaction_type AS ENUM ('DEPOSIT', 'WITHDRAW', 'TRANSFER');
CREATE TYPE transaction_status AS ENUM ('PENDING', 'SUCCESS', 'FAILED');
CREATE TABLE transactions
(
    id           BIGSERIAL PRIMARY KEY,
    wallet_id    BIGINT NOT NULL REFERENCES wallets(id) ON DELETE CASCADE,
    amount       BIGINT NOT NULL CHECK (amount <> 0),
    type         transaction_type NOT NULL,
    status       transaction_status NOT NULL,
    reference_id TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_wallet_id ON transactions(wallet_id);
CREATE INDEX idx_transactions_wallet_created_at ON transactions (wallet_id, created_at DESC);

CREATE UNIQUE INDEX idx_transactions_ref_unique ON transactions(reference_id)
WHERE reference_id IS NOT NULL;