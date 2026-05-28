-- FrutApp · Verificación de correo al registrarse
-- Nueva columna: las cuentas nuevas nacen sin verificar; las existentes se marcan
-- como verificadas para no romper sus inicios de sesión.
ALTER TABLE app_user ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
UPDATE app_user SET email_verified = true;

-- Códigos de verificación de correo: se guarda solo el hash del código.
CREATE TABLE email_verification_token (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    code_hash   TEXT        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_email_verification_user ON email_verification_token (user_id);
