-- FrutApp · Cupones FrutCoins (canje real del ledger)
--
-- Hasta ahora el saldo y los movimientos vivian en frutcoins_ledger (V4) pero
-- el canje desde la app era un mock local (RewardsStore.spend). Esta tabla
-- materializa los canjes como cupones: un codigo unico que el cliente puede
-- usar / mostrar / compartir, con monto debitado, descripcion de la
-- recompensa, estado (activo/usado/expirado) y fecha de vencimiento.
--
-- Un canje crea: (1) fila en frutcoins_ledger con delta negativo motivo=CANJE
-- referenciando el cupon, (2) fila en cupon. Operacion atomica en BD.
--
-- idempotency_key (UNIQUE per user) deja al cliente reintentar el POST
-- /v1/frutcoins/redeem sin duplicar el cupon: si llega 2 veces el mismo
-- key, devolvemos el cupon ya creado en vez de hacer doble debito.

CREATE TABLE cupon (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES app_user (id),
    codigo          TEXT         NOT NULL UNIQUE,
    monto           INTEGER      NOT NULL CHECK (monto > 0),
    recompensa      TEXT         NOT NULL,    -- ej "Descuento $1000 en tu próximo pedido"
    estado          TEXT         NOT NULL DEFAULT 'ACTIVO', -- ACTIVO/USADO/EXPIRADO
    idempotency_key TEXT         NOT NULL,
    expira_en       TIMESTAMPTZ,
    usado_en        TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_cupon_user_idempotency ON cupon (user_id, idempotency_key);
CREATE INDEX ix_cupon_user ON cupon (user_id, created_at DESC);

-- Vinculamos el movimiento del ledger al cupon (audit trail).
ALTER TABLE frutcoins_ledger
    ADD COLUMN cupon_id UUID REFERENCES cupon (id);

COMMENT ON TABLE cupon IS
    'Cupones generados al canjear FrutCoins. codigo es la clave que ve el cliente; idempotency_key blinda reintentos del POST.';
