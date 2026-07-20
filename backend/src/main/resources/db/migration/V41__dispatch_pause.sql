-- FrutApp · Pausa de despacho (repartidor)
--
-- El repartidor puede pausar la entrega en vuelo (semaforo largo, tuvo que
-- parar en el super, emergencia). El pedido sigue en EN_DESPACHO pero
-- expone una razon + timestamp al cliente para que sepa por que no avanza
-- el marker del mapa.
--
-- Diseñado como FLAG (no estado nuevo) porque:
--  1. No rompe la maquina de estados existente (V4) ni la retrocompat del
--     APK desplegado (dispatch_paused_at NULL = "no pausado", comportamiento
--     historico).
--  2. Simplifica el back-office: sigue existiendo un solo pedido en despacho,
--     no dos "estados de despacho" a manejar.
--  3. Si mañana necesitamos que el sistema tome accion sobre pausas largas
--     (auto-notificar soporte, escalar), es una query simple sobre este
--     timestamp.
--
-- ADITIVO Y SEGURO: APK pre-v0.1.14 no lee los nuevos campos, no rompe nada.

ALTER TABLE customer_order
    ADD COLUMN dispatch_paused_at TIMESTAMPTZ,
    ADD COLUMN dispatch_pause_reason TEXT;

-- Indice parcial: query "todos los pedidos pausados ahora" (para el back-office
-- o para el auto-reanudar si a futuro lo agregamos). Solo indexa filas
-- pausadas → cero costo para el 99% de pedidos.
CREATE INDEX IF NOT EXISTS ix_customer_order_dispatch_paused
    ON customer_order (dispatch_paused_at DESC)
    WHERE dispatch_paused_at IS NOT NULL;

COMMENT ON COLUMN customer_order.dispatch_paused_at IS
    'Timestamp cuando el repartidor pauso el despacho. NULL = no pausado. Se limpia al reanudar o al pasar a ENTREGADO/CANCELADO.';
COMMENT ON COLUMN customer_order.dispatch_pause_reason IS
    'Razon de la pausa (semaforo, emergencia, otro). Solo visible mientras dispatch_paused_at != NULL.';
