-- V14: inbox de notificaciones del usuario.
--
-- Por que: el NotificationDispatcher hoy envia push via FCM pero no deja
-- traza. El front mostraba notifs mockeadas en NotificacionesScreen. Para
-- que el inbox in-app sea consistente entre devices (y sobreviva
-- reinstall), persistimos cada push como una fila acá ANTES de enviar al
-- FCM. La pantalla del cliente lee de `GET /v1/notifications` (no del
-- store local).
--
-- type: catalogo simple del enum cliente (PEDIDO/COINS/RECICLA/RACHA/PROMO).
--       No es FK porque los tipos pueden cambiar con el copy del producto
--       y no queremos migracion data cada vez. Validacion en codigo.
-- data: JSONB con payload contextual (orderId, deltaCoins, etc). Permite
--       deep-link al tap en el front sin tener que serializar mas columnas.
-- read_at: NULL = no leida. La pantalla cuenta los NULL para el badge.

CREATE TABLE notification (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    type        TEXT         NOT NULL,
    title       TEXT         NOT NULL,
    body        TEXT         NOT NULL,
    data        JSONB,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    read_at     TIMESTAMPTZ
);

-- Query mas comun: listar las del user ordenadas mas recientes primero.
CREATE INDEX ix_notification_user_recent ON notification (user_id, created_at DESC);

-- Conteo de no leidas para el badge — indice parcial = mucho mas chico.
CREATE INDEX ix_notification_user_unread ON notification (user_id) WHERE read_at IS NULL;
