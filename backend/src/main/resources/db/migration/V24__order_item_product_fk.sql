-- FrutApp · FK formal de order_item.product_id -> product.id
--
-- V4 dejo product_id como "referencia logica" (UUID NOT NULL sin FK) con la
-- intencion de "si en el futuro borramos productos del catalogo, los pedidos
-- viejos siguen viendose por el snapshot (nombre/precio/image_key)". El
-- argumento se sostiene PARA EL VISUAL del pedido, pero deja la BD sin
-- proteccion contra:
--   - DELETE accidental de product (un script de soporte, un bug) que rompe
--     reportes que joinean a product (no se trata aqui, pero el riesgo va a
--     ir creciendo a medida que sumemos endpoints admin).
--   - INSERT en order_item con product_id apuntando a la nada (un bug del
--     OrderService) que queda silencioso hasta que un test rompe el join.
--   - Falta de garantia para queries que asumen el join (ej. "stock por
--     producto vendido hoy").
--
-- Solucion: agregar FK con ON DELETE RESTRICT. Si alguien intenta borrar un
-- product que tiene order_items asociados, Postgres lo rechaza con un error
-- claro. El soft delete (product.deleted_at) sigue siendo el camino normal
-- para "ocultar" un producto sin afectar pedidos viejos.
--
-- RESTRICT vs CASCADE: NUNCA cascade aqui. Borrar un producto JAMAS debe
-- borrar los pedidos del cliente. Eso seria perder historial transaccional.
-- RESTRICT vs SET NULL: SET NULL tendria que volver product_id NULLABLE, lo
-- que diluye la garantia "todo item de pedido tiene un producto en el catalogo".
-- Preferimos RESTRICT para que la decision de borrar quede explicita y
-- requiera intervencion (en cuyo caso, soft delete es la respuesta).
--
-- Impacto operativo: si esta migration FALLA con
-- "violates foreign key constraint" significa que ya hay order_item con
-- product_id huerfano (apuntando a un product inexistente). En ese caso hay
-- que hacer cleanup manual en QA antes de re-correr (no hay borrado real de
-- product en codigo hoy, asi que no deberia pasar).

ALTER TABLE order_item
    ADD CONSTRAINT fk_order_item_product
    FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT;
