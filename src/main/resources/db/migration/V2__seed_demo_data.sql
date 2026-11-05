-- =====================================================================
-- DML: datos de demostración (para probar la API desde el primer arranque)
-- =====================================================================

INSERT INTO clients (identification_type, identification_number, first_name, last_name, email, birth_date, created_at)
VALUES ('CC', '1075000001', 'Laura',  'Gomez',   'laura.gomez@example.com',   '1992-03-14', (NOW() AT TIME ZONE 'America/Bogota')),
       ('CC', '1075000002', 'Carlos', 'Ramirez', 'carlos.ramirez@example.com', '1988-09-30', (NOW() AT TIME ZONE 'America/Bogota'));

INSERT INTO accounts (account_type, account_number, status, balance, available_balance, gmf_exempt, created_at, client_id)
SELECT 'SAVINGS', '5300000001', 'ACTIVE', 500000.00, 500000.00, FALSE, (NOW() AT TIME ZONE 'America/Bogota'), id
FROM clients WHERE identification_number = '1075000001';

-- Cuenta corriente: saldo disponible = saldo + cupo de sobregiro (1.000.000)
INSERT INTO accounts (account_type, account_number, status, balance, available_balance, gmf_exempt, created_at, client_id)
SELECT 'CHECKING', '3300000001', 'ACTIVE', 1000000.00, 2000000.00, TRUE, (NOW() AT TIME ZONE 'America/Bogota'), id
FROM clients WHERE identification_number = '1075000001';

INSERT INTO accounts (account_type, account_number, status, balance, available_balance, gmf_exempt, created_at, client_id)
SELECT 'SAVINGS', '5300000002', 'ACTIVE', 250000.00, 250000.00, FALSE, (NOW() AT TIME ZONE 'America/Bogota'), id
FROM clients WHERE identification_number = '1075000002';

-- Movimiento de saldo inicial por cada cuenta sembrada
INSERT INTO transactions (reference, transaction_type, movement_type, amount, balance_after,
                          available_balance_after, account_id, account_number, description, created_at)
SELECT gen_random_uuid()::text, 'DEPOSIT', 'CREDIT', balance, balance, available_balance,
       id, account_number, 'Saldo inicial', (NOW() AT TIME ZONE 'America/Bogota')
FROM accounts;
