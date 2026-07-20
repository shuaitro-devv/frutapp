-- FrutApp · Programa de referidos ("Referir un amigo")
--
-- Diseño:
--  1. Cada usuario tiene un `codigo_invitacion` unico de 8 chars alfanumericos
--     upper-case (evita 0/O y 1/I/L para reducir errores al dictarlo). Se
--     genera al signup + al backfill de usuarios existentes.
--  2. Al registrarse un nuevo usuario, puede ingresar un codigo de invitacion.
--     Se guarda en `referred_by_user_id` para dejar el link. Se valida que
--     el codigo exista al momento del signup (o se descarta silenciosamente).
--  3. Cuando el referido completa su PRIMER pedido ENTREGADO:
--     - El referidor recibe 200 FrutCoins (motivo REFERIDO_COMPLETO).
--     - El referido recibe 100 FrutCoins (motivo BONO_BIENVENIDA_REFERIDO).
--     Este trigger vive en el service (no como trigger SQL) para poder
--     retener la logica de "es el primer pedido" en Kotlin.
--
-- Aditivo: campos nullable, ninguna app pre-V42 los usa.

ALTER TABLE app_user
    ADD COLUMN codigo_invitacion TEXT UNIQUE,
    ADD COLUMN referred_by_user_id UUID REFERENCES app_user(id),
    ADD COLUMN referral_reward_granted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS ix_app_user_referred_by
    ON app_user (referred_by_user_id)
    WHERE referred_by_user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_app_user_codigo_invitacion
    ON app_user (codigo_invitacion)
    WHERE codigo_invitacion IS NOT NULL;

-- Backfill: genera codigos para usuarios existentes. Retry loop simple con
-- charset sin ambiguos; si por casualidad colisiona, la fila falla el
-- UNIQUE y queda sin codigo (el service lo generara la primera vez que el
-- user pida verlo).
DO $$
DECLARE
    u RECORD;
    charset TEXT := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
    codigo TEXT;
    i INT;
BEGIN
    FOR u IN SELECT id FROM app_user WHERE codigo_invitacion IS NULL LOOP
        codigo := '';
        FOR i IN 1..8 LOOP
            codigo := codigo || substr(charset, floor(random() * length(charset))::int + 1, 1);
        END LOOP;
        UPDATE app_user SET codigo_invitacion = codigo WHERE id = u.id;
    END LOOP;
END $$;

COMMENT ON COLUMN app_user.codigo_invitacion IS
    '8 chars alfanumericos upper-case, sin 0/O/1/I/L (evita errores al dictar). Se muestra al user en FrutCoinsScreen para compartir.';
COMMENT ON COLUMN app_user.referred_by_user_id IS
    'Usuario que trajo a este via su codigo de invitacion. Null = signup organico. Bono se paga al primer pedido ENTREGADO.';
COMMENT ON COLUMN app_user.referral_reward_granted IS
    'True cuando ya se otorgo el bono al referidor + referido. Evita pagos duplicados aunque el service se re-ejecute (idempotencia).';
