-- FrutApp · Resenas de producto por cliente
--
-- Una resena por (user, producto): upsert via UNIQUE(product_id, user_id). El
-- texto puede ser vacio (rating-solo), pero las estrellas son obligatorias en
-- 1..5. Soft delete del usuario / producto NO afecta la fila (FK ON DELETE
-- CASCADE en hard delete — los soft delete dejan la resena vigente para no
-- perder historial visible en otros usuarios).
--
-- El primer flow viene de CalificarPedidoScreen: al confirmar la entrega del
-- pedido, el cliente puede dejar 1-5 estrellas + comentario opcional para
-- cada item. Esa pantalla mapea order_item.image_key a product.id y guarda
-- una resena por cada producto del pedido.
--
-- updated_at se actualiza en el upsert; created_at se mantiene del primer
-- POST (asi "editar" no salta arriba de la lista cuando ordenamos por fecha).

CREATE TABLE producto_resena (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID        NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    estrellas   INT         NOT NULL CHECK (estrellas BETWEEN 1 AND 5),
    texto       TEXT        NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (product_id, user_id)
);

-- Query frecuente: "resenas de este producto, las mas nuevas arriba". Cubre
-- tanto la pantalla detalle de producto (LIMIT 3 visible) como la screen full.
CREATE INDEX ix_producto_resena_product_time
    ON producto_resena (product_id, created_at DESC);

COMMENT ON TABLE producto_resena IS
    'Resena 1-5 estrellas + texto opcional del cliente al producto. Una por (product_id, user_id); upsert al editar.';
