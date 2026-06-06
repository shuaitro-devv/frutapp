-- ============================================================================
-- V11 - Auditoria de actividad de usuarios
-- ============================================================================
-- Cubre tres preguntas operativas y regulatorias clave:
--   1. "¿Quien hizo que en este pedido?"  -> agregar actor_user_id en
--      order_status_history (hoy solo tiene actor TEXT generico).
--   2. "¿Que hicieron los usuarios?"      -> tabla user_event como ledger de
--      eventos relevantes (login, register, order_*, frutcoins_*, etc).
--   3. "¿Desde donde y cuando entran?"    -> contexto tecnico (IP, UA,
--      app_version, device_model) por evento.
--
-- Diseno alineado con la Ley 21.719: cada evento tiene proposito declarado
-- (event_type), conservacion auditable (created_at) y permite anonimizar
-- (UPDATE user_id=NULL) sin perder estadisticas.
-- ============================================================================

-- 1) Actor identificado en las transiciones de estado del pedido.
-- Hoy order_status_history.actor solo guarda 'CLIENTE/SISTEMA/OPERADOR/REPARTIDOR'.
-- Agregamos QUIEN exactamente fue (nullable porque transiciones viejas y las
-- del actor 'SISTEMA' no aplica).
ALTER TABLE order_status_history
    ADD COLUMN actor_user_id UUID REFERENCES app_user (id);

CREATE INDEX ix_order_history_actor ON order_status_history (actor_user_id)
    WHERE actor_user_id IS NOT NULL;


-- 2) Ledger central de eventos de usuario.
-- Patron event-sourced (como frutcoins_ledger): nunca update, solo insert.
-- payload JSONB para detalles especificos del evento sin schema rigido.
CREATE TABLE user_event (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID        REFERENCES app_user (id) ON DELETE SET NULL,
    -- ^ nullable para eventos anonimos (browse pre-login) Y para anonimizacion
    --   post-retencion (perdemos el quien pero conservamos el estadistico).

    event_type   TEXT        NOT NULL,
    -- ^ vocabulario controlado. Convencion: <dominio>.<accion>. Ej:
    --   'auth.login_ok', 'auth.login_fail', 'auth.register', 'auth.logout',
    --   'order.created', 'order.cancelled', 'order.completed',
    --   'staff.order_taken', 'staff.order_released', 'staff.order_completed',
    --   'staff.dispatch_taken', 'staff.dispatch_delivered',
    --   'frutcoins.earned', 'frutcoins.redeemed',
    --   'product.viewed', 'cart.added' (cuando enchufemos eventos UX).

    entity_type  TEXT,
    -- ^ 'order', 'product', 'user', etc. Para filtrar 'todo lo que paso
    --   con el pedido X' sin recorrer el payload.
    entity_id    UUID,

    payload      TEXT        NOT NULL DEFAULT '{}',
    -- ^ contexto especifico serializado a JSON. Lo guardamos como TEXT (no JSONB)
    --   para que Exposed pueda escribirlo via setString sin cast explicito; cuando
    --   necesitemos indices JSONB (busquedas por contenido) migramos en V13+ con
    --   ALTER COLUMN ... USING payload::jsonb. Ej: auth.login_fail: {"email":"x","motivo":"y"}.

    -- Contexto tecnico (de donde viene el evento). TEXT en vez de INET para
    -- aceptar hostnames de proxies y valores no estandar sin que el INSERT
    -- crashee por parse de inet. Anonimizacion se hace en codigo (mascara antes
    -- de insertar) cuando hagamos el job de retencion de 90 dias.
    ip_address   TEXT,
    user_agent   TEXT,
    app_version  TEXT,
    device_model TEXT,
    os_version   TEXT,
    locale       TEXT,
    channel      TEXT,
    -- ^ 'android', 'ios', 'web', 'api'. Para segmentar por canal.

    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indices: por user (cronologico) + por tipo (analytics) + por entidad
-- (auditoria del pedido X) + por tiempo (jobs de retencion).
CREATE INDEX ix_user_event_user        ON user_event (user_id, created_at DESC);
CREATE INDEX ix_user_event_type        ON user_event (event_type, created_at DESC);
CREATE INDEX ix_user_event_entity      ON user_event (entity_type, entity_id) WHERE entity_id IS NOT NULL;
CREATE INDEX ix_user_event_created     ON user_event (created_at);

COMMENT ON TABLE user_event IS
    'Ledger inmutable de eventos de usuario para auditoria, soporte y analytics. Nunca UPDATE, solo INSERT.';
