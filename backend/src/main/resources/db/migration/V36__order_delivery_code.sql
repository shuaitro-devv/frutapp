-- FrutApp · Codigo de entrega del pedido (handshake fisico cliente↔repartidor)
--
-- Hasta ahora el repartidor podia tocar "Confirmar entrega" sin haber llegado
-- al cliente — la pantalla mostraba un codigo hardcoded "4821" y el backend
-- transicionaba a ENTREGADO sin validar nada.
--
-- Con esta columna:
--   1. Al transicionar el pedido a EN_DESPACHO (repartidor lo toma) generamos
--      un codigo de 4 digitos y lo guardamos aca.
--   2. El cliente ve el codigo SOLO en SU app (GET /v1/orders/{id} se lo
--      devuelve; endpoints de staff NO).
--   3. Al "Confirmar entrega", el repartidor envia el codigo. El backend
--      compara con el guardado: si no coincide → 400 "Codigo incorrecto".
--
-- Para retro-compat: pedidos pre-migracion quedan con NULL. El endpoint
-- aceptara codigo opcional si la fila no tiene codigo guardado (no obliga
-- a regenerar codigos retroactivamente).
--
-- Formato: 4 digitos numericos (1000-9999). Suficiente para piloto; si
-- crecemos, se puede subir a 6 sin migrar (solo cambiar el rango).

ALTER TABLE customer_order
    ADD COLUMN delivery_code TEXT;

COMMENT ON COLUMN customer_order.delivery_code IS
    'Codigo de 4 digitos generado al EN_DESPACHO. El cliente lo ve, el repartidor lo pide y lo manda al confirmar entrega. Backend valida match antes de transicionar a ENTREGADO.';
