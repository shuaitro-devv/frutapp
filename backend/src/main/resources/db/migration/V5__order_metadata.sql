-- FrutApp · Metadata del pedido: modalidad de entrega, contexto del cliente y pagos (split)

-- Modalidad + contexto (todo opcional salvo la modalidad, que tiene default).
ALTER TABLE customer_order
    ADD COLUMN fulfillment_type TEXT NOT NULL DEFAULT 'DELIVERY',  -- DELIVERY | RETIRO
    ADD COLUMN sucursal         TEXT,                              -- solo RETIRO
    ADD COLUMN channel          TEXT,                              -- APP_ANDROID, WEB, ...
    ADD COLUMN app_version      TEXT,
    ADD COLUMN device_model     TEXT,
    ADD COLUMN os_version       TEXT,
    ADD COLUMN locale           TEXT;

-- Pagos como LISTA: un pedido puede tener más de un medio (pago dividido, incl. FrutCoins).
CREATE TABLE order_payment (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id   UUID        NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    method     TEXT        NOT NULL,   -- TARJETA/WEBPAY/MERCADO_PAGO/EFECTIVO/FRUTCOINS/...
    monto      INTEGER     NOT NULL,   -- CLP que cubre este medio
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_order_payment_order ON order_payment (order_id);
