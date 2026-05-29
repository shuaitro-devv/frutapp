-- FrutApp · Pedidos (máquina de estados) + ledger de FrutCoins
-- "order" es palabra reservada en SQL -> la tabla se llama customer_order.

CREATE TABLE customer_order (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    numero              TEXT        NOT NULL UNIQUE,          -- ej #FRU-2026-000123 (display)
    user_id             UUID        NOT NULL REFERENCES app_user (id),
    status              TEXT        NOT NULL,                 -- CREADO, PAGADO, EN_PICKING, ...
    payment_status      TEXT        NOT NULL,                 -- PREAUTORIZADO, CAPTURADO, REEMBOLSADO
    direccion           TEXT        NOT NULL,                 -- snapshot al pedir
    entrega             TEXT        NOT NULL,                 -- ventana estimada (texto en MVP)
    subtotal_estimado   INTEGER     NOT NULL,
    envio               INTEGER     NOT NULL,
    total_estimado      INTEGER     NOT NULL,
    total_final         INTEGER,                              -- null hasta STOCK_CONFIRMADO
    frutcoins_ganadas   INTEGER     NOT NULL DEFAULT 0,
    frutcoins_canjeadas INTEGER     NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at          TIMESTAMPTZ
);

CREATE TABLE order_item (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id         UUID    NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    product_id       UUID    NOT NULL,                        -- referencia lógica
    nombre           TEXT    NOT NULL,                        -- snapshot
    unidad           TEXT    NOT NULL,                        -- snapshot (kg/unidad/atado)
    image_key        TEXT    NOT NULL,                        -- snapshot
    precio_unitario  INTEGER NOT NULL,                        -- snapshot al pedir (por unidad/kg)
    gramos           INTEGER,                                 -- solo productos por kg (null = por unidad)
    cantidad         INTEGER NOT NULL,
    monto_estimado   INTEGER NOT NULL,
    peso_real        INTEGER,                                 -- null hasta confirmar stock
    monto_final      INTEGER,
    item_status      TEXT    NOT NULL DEFAULT 'PENDIENTE'     -- PENDIENTE/CONFIRMADO/SUSTITUIDO/SIN_STOCK
);

CREATE TABLE order_status_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID        NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    from_status TEXT,
    to_status   TEXT        NOT NULL,
    actor       TEXT        NOT NULL,                          -- CLIENTE/SISTEMA/OPERADOR/REPARTIDOR
    nota        TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- FrutCoins como LEDGER (saldo monetario auditable), no un contador.
CREATE TABLE frutcoins_ledger (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES app_user (id),
    order_id      UUID        REFERENCES customer_order (id),
    delta         INTEGER     NOT NULL,                        -- + ganadas / - canjeadas
    motivo        TEXT        NOT NULL,                         -- COMPRA/CANJE/REEMBOLSO/AJUSTE
    balance_after INTEGER     NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_order_user ON customer_order (user_id);
CREATE INDEX ix_order_item_order ON order_item (order_id);
CREATE INDEX ix_order_history_order ON order_status_history (order_id);
CREATE INDEX ix_frutcoins_user ON frutcoins_ledger (user_id);
