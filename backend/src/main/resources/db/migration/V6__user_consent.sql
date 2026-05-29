-- FrutApp · Registro del consentimiento de T&C / Política de Privacidad al crear la cuenta.
ALTER TABLE app_user
    ADD COLUMN consent_version TEXT,        -- version de T&C/Politica aceptada
    ADD COLUMN consent_at      TIMESTAMPTZ; -- cuando se acepto
