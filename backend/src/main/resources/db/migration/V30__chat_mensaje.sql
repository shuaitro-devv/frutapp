-- FrutApp · Chat in-app por pedido (cliente <-> picker <-> repartidor)
--
-- Modelo de conversacion: UNA conversacion por pedido. Cada mensaje guarda
-- el rol del autor (cliente/picker/repartidor) y el rol destinatario
-- (picker o repartidor). Asi:
--   - El historial agrupa todo lo del pedido en un solo lugar (soporte ve
--     contexto completo al investigar).
--   - El cliente puede filtrar "mensajes con picker" vs "con repartidor".
--   - Picker <-> repartidor no se soporta todavia (no entra en el flujo
--     coordinado del piloto); cuando se sume, basta otro destinatario_rol.
--
-- Realtime via WebSocket: el backend mantiene un Hub en memoria con las
-- conexiones por orderId; cuando alguien postea, se broadcastea a los
-- conectados. Si el destinatario NO esta conectado, se manda FCM data-only
-- con `type=chat_mensaje` y `orderId` para que la app abra el chat al tap.
--
-- leido_en: el destinatario lo marca leido cuando entra al chat. Asi el
-- cliente puede ver "Visto" tipo WhatsApp y el otro lado puede mostrar
-- un badge con cantidad de no-leidos.
--
-- cuerpo: max 1000 chars validado en codigo. Sin attachments en V1 (foto
-- evidencia ya cubre la prueba visual del item; chat con texto suficiente
-- para coordinar entregas).

CREATE TABLE chat_mensaje (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id          UUID        NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    autor_user_id     UUID        NOT NULL REFERENCES app_user (id),
    autor_rol         TEXT        NOT NULL,  -- cliente / picker / repartidor
    destinatario_rol  TEXT        NOT NULL,  -- picker / repartidor
    cuerpo            TEXT        NOT NULL,
    leido_en          TIMESTAMPTZ,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- El query mas frecuente sera "historial de este pedido ordenado por tiempo"
-- (al abrir el chat) y "mensajes nuevos desde X" (poll incremental o fallback
-- si el WS no se conectara). Un indice por (order_id, created_at) cubre ambos.
CREATE INDEX ix_chat_mensaje_order_time
    ON chat_mensaje (order_id, created_at);

-- Para "no leidos" del cliente sin escanear todo el historial.
CREATE INDEX ix_chat_mensaje_unread
    ON chat_mensaje (order_id, destinatario_rol)
    WHERE leido_en IS NULL;

COMMENT ON TABLE chat_mensaje IS
    'Mensajes del chat in-app por pedido. Conversacion unica por order_id; el rol del autor y del destinatario distinguen el flujo cliente<->picker vs cliente<->repartidor.';
