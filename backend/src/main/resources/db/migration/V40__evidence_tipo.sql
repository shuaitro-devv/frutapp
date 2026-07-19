-- FrutApp · Evidencia con tipo explicito (foto vs firma del receptor)
--
-- Hasta V38 la evidencia solo era "foto por item" (picker) o "foto por
-- pedido" (repartidor, con order_item_id NULL). Ahora sumamos firma del
-- receptor, que tambien va a order-level pero NO es una foto — es un
-- PNG con los trazos del cliente firmando en pantalla.
--
-- Necesitamos distinguir "foto de entrega" de "firma de entrega" para
-- que el cliente en su tracking los muestre en cards separadas y
-- soporte usar cada uno por separado.
--
-- Aditivo y seguro para el APK:
--   - tipo TEXT NULL: filas viejas quedan con NULL, el backend responde
--     null en el DTO y el cliente/repartidor lo interpretan como
--     'DELIVERY_PHOTO' cuando order_item_id es null (comportamiento
--     historico).
--   - Los INSERT nuevos setean tipo='DELIVERY_PHOTO' o 'DELIVERY_SIGNATURE'.
--   - No hay CHECK constraint (evita romper migrations futuras).

ALTER TABLE order_item_evidence
    ADD COLUMN tipo TEXT;

-- No creamos indice adicional: la unica query cliente (listByOrder) trae
-- toda la evidencia del pedido y filtra en memoria. El indice existente
-- ix_order_item_evidence_order (V25) cubre esa ruta. Cuando aparezca una
-- query especifica por tipo, ahi si conviene un partial index.

COMMENT ON COLUMN order_item_evidence.tipo IS
    'null (legacy) | DELIVERY_PHOTO | DELIVERY_SIGNATURE. Segun este campo el cliente y el back-office renderizan cards distintas.';
