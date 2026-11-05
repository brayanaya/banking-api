-- =====================================================================
-- DDL: estructuras de persistencia (clientes, cuentas, transacciones)
-- =====================================================================

CREATE TABLE clients (
    id                    BIGSERIAL PRIMARY KEY,
    identification_type   VARCHAR(20)  NOT NULL,
    identification_number VARCHAR(20)  NOT NULL,
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100) NOT NULL,
    email                 VARCHAR(150) NOT NULL,
    birth_date            DATE         NOT NULL,
    created_at            TIMESTAMP    NOT NULL,
    updated_at            TIMESTAMP,
    CONSTRAINT ck_clients_identification_type CHECK (identification_type IN ('CC', 'CE', 'PASSPORT', 'NIT')),
    CONSTRAINT ck_clients_first_name_len CHECK (char_length(btrim(first_name)) >= 2),
    CONSTRAINT ck_clients_last_name_len  CHECK (char_length(btrim(last_name)) >= 2),
    CONSTRAINT ck_clients_email_format   CHECK (email ~* '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$'),
    CONSTRAINT uk_clients_identification UNIQUE (identification_type, identification_number),
    CONSTRAINT uk_clients_email UNIQUE (email)
);

-- Regla: un cliente no puede existir en la base de datos si es menor de edad.
-- Se usa un trigger (y no un CHECK) porque depende de la fecha actual (no inmutable).
CREATE OR REPLACE FUNCTION fn_check_client_adult() RETURNS TRIGGER AS $$
BEGIN
    IF NEW.birth_date > (CURRENT_DATE - INTERVAL '18 years')::date THEN
        RAISE EXCEPTION 'El cliente debe ser mayor de edad (birth_date=%)', NEW.birth_date;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_clients_adult
    BEFORE INSERT OR UPDATE OF birth_date ON clients
    FOR EACH ROW EXECUTE FUNCTION fn_check_client_adult();

CREATE TABLE accounts (
    id                BIGSERIAL PRIMARY KEY,
    account_type      VARCHAR(20)    NOT NULL,
    account_number    VARCHAR(10)    NOT NULL,
    status            VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    balance           NUMERIC(19, 2) NOT NULL DEFAULT 0,
    available_balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    gmf_exempt        BOOLEAN        NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP      NOT NULL,
    updated_at        TIMESTAMP,
    client_id         BIGINT         NOT NULL,
    CONSTRAINT fk_accounts_client FOREIGN KEY (client_id) REFERENCES clients (id) ON DELETE RESTRICT,
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    CONSTRAINT ck_accounts_type   CHECK (account_type IN ('CHECKING', 'SAVINGS')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'CANCELLED')),
    CONSTRAINT ck_accounts_number_format CHECK (
        account_number ~ '^[0-9]{10}$'
        AND ((account_type = 'SAVINGS'  AND account_number LIKE '53%')
          OR (account_type = 'CHECKING' AND account_number LIKE '33%'))
    ),
    -- La cuenta de ahorros no puede tener saldo menor a 0
    CONSTRAINT ck_accounts_savings_balance CHECK (account_type <> 'SAVINGS' OR balance >= 0)
);

CREATE INDEX idx_accounts_client_id ON accounts (client_id);

CREATE TABLE transactions (
    id                      BIGSERIAL PRIMARY KEY,
    reference               VARCHAR(36)    NOT NULL,
    transaction_type        VARCHAR(20)    NOT NULL,
    movement_type           VARCHAR(10)    NOT NULL,
    amount                  NUMERIC(19, 2) NOT NULL,
    balance_after           NUMERIC(19, 2) NOT NULL,
    available_balance_after NUMERIC(19, 2) NOT NULL,
    account_id              BIGINT         NOT NULL,
    account_number          VARCHAR(10)    NOT NULL,
    related_account_number  VARCHAR(10),
    description             VARCHAR(255),
    created_at              TIMESTAMP      NOT NULL,
    CONSTRAINT fk_transactions_account FOREIGN KEY (account_id) REFERENCES accounts (id) ON DELETE RESTRICT,
    CONSTRAINT ck_transactions_type     CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT ck_transactions_movement CHECK (movement_type IN ('CREDIT', 'DEBIT')),
    CONSTRAINT ck_transactions_amount   CHECK (amount > 0)
);

CREATE INDEX idx_transactions_account_date ON transactions (account_id, created_at);
CREATE INDEX idx_transactions_reference    ON transactions (reference);
