-- FrutApp · Evidencia de entrega (repartidor)
--
-- Antes esta tabla solo guardaba fotos del picker por ITEM (V25). Ahora tambien
-- guardamos la foto que el repartidor saca del paquete YA ENTREGADO — es evidencia
-- del pedido completo, no de un item, asi que `order_item_id` pasa a ser nullable:
--   - NOT NULL = foto del picker asociada a un item concreto
--   - NULL     = foto del repartidor asociada al pedido completo (entrega)
--
-- Aditivo y seguro para el APK desplegado: la query "por item" hace WHERE item_id = X,
-- las filas NULL simplemente no matchean; el cliente actual sigue viendo lo mismo.

ALTER TABLE order_item_evidence
    ALTER COLUMN order_item_id DROP NOT NULL;

-- Indice para la nueva query "fotos de entrega de este pedido"
-- (WHERE order_id = X AND order_item_id IS NULL). El indice existente
-- ix_order_item_evidence_order ya cubre order_id + uploaded_at DESC, pero
-- este partial es mas barato para el filtro exacto por entrega.
CREATE INDEX IF NOT EXISTS ix_order_item_evidence_delivery
    ON order_item_evidence (order_id, uploaded_at DESC)
    WHERE order_item_id IS NULL;

COMMENT ON COLUMN order_item_evidence.order_item_id IS
    'null cuando la foto es del pedido completo (entrega del repartidor); NOT NULL cuando es del picker por item.';
