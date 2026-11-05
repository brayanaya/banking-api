-- =====================================================================
-- V3: Fortalecer validación de email (TLD mínimo 2 caracteres)
-- =====================================================================

-- Eliminar el CHECK anterior y crear uno más estricto
ALTER TABLE clients DROP CONSTRAINT IF EXISTS ck_clients_email_format;

ALTER TABLE clients ADD CONSTRAINT ck_clients_email_format
    CHECK (email ~* '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]{2,}$');