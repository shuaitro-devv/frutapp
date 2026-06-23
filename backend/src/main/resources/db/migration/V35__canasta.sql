-- FrutApp · Canastas guardadas del cliente
--
-- El cliente puede guardar combinaciones de productos como "canasta" (Asado,
-- Fitness, Mi Semanal, etc) para recomprar en 1 tap. Hasta ahora vivian en
-- memoria (CanastaStore), se perdian al cerrar la app y no se podian abrir
-- en otro celular ni compartir entre miembros del hogar.
--
-- Esquema:
--   canasta       (id, user_id, nombre, emoji, recordatorio_mensual, fechas)
--   canasta_item  (id, canasta_id, product_id, cantidad, gramos, posicion)
--
-- `posicion` permite ordenamiento estable cuando el cliente reordena items
-- desde la UI; default 0 mantiene insercion FIFO.
--
-- Templates (Canasta Asado / Fitness / etc) NO se persisten en BD — viven
-- en el cliente (catalogo curado, listo para "copiar template"). Solo las
-- canastas creadas por el usuario llegan aca.

CREATE TABLE canasta (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id               UUID         NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    nombre                TEXT         NOT NULL,
    emoji                 TEXT         NOT NULL DEFAULT '🧺',
    recordatorio_mensual  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_canasta_user_time ON canasta (user_id, created_at DESC);

CREATE TABLE canasta_item (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    canasta_id  UUID         NOT NULL REFERENCES canasta (id) ON DELETE CASCADE,
    product_id  UUID         NOT NULL REFERENCES product (id),
    cantidad    INT          NOT NULL CHECK (cantidad > 0),
    gramos      INT          CHECK (gramos IS NULL OR gramos > 0),
    posicion    INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX ix_canasta_item_canasta ON canasta_item (canasta_id, posicion);

COMMENT ON TABLE canasta IS
    'Canastas guardadas del cliente. Templates (Asado/Fitness/etc) NO van aca — viven en el front.';
