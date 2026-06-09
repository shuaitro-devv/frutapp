-- V13: tabla device_token para FCM (push notifications).
--
-- Modelo:
-- - Cada device guarda 1 token de FCM (lo regenera el SDK; Android lo provee
--   en cada arranque tras instalar). Cuando el SDK avisa onNewToken la app
--   manda POST /v1/device/token; el backend hace upsert por fcm_token
--   (que es globalmente unico en FCM) — eso resuelve el caso "Juan cierra
--   sesion y Maria se loguea en el mismo celu": el token reaparece con
--   otro user_id y la fila se reasigna (sino, Maria recibiria push de Juan).
-- - Borrado: el endpoint DELETE limpia al hacer logout; si FCM responde
--   UNREGISTERED al enviar, el dispatcher borra el token tambien.
-- - platform: ANDROID/IOS/WEB para futuro multi-plataforma (hoy solo Android).
-- - app_id: el applicationId concreto (cl.frutapp.app, cl.frutapp.app.debug,
--   cl.frutapp.app.sofruco, etc.). Permite enviar push solo a la app del
--   brand correcto cuando un mismo user tiene FrutApp + Sofruco instaladas.
CREATE TABLE device_token (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    fcm_token   TEXT         NOT NULL UNIQUE,
    platform    TEXT         NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
    app_id      TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Query mas comun: enviar push a un user => listar sus tokens activos.
CREATE INDEX ix_device_token_user ON device_token (user_id);
