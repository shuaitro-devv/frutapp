-- FrutApp · Feature flags via app_config
--
-- Aprovechamos la infraestructura existente de `app_config` (V7) para gatear
-- funcionalidades on/off sin redeploy. Mismo refresh periodico que el resto
-- de la config (60s en backend; cada llamada en app via ConfigStore).
--
-- Convencion de naming:
--   business.*   - valores numericos/strings de negocio (envio_gratis, costo_envio,
--                  peso_tolerancia, frutcoins_max_porc_pago). Ya existian asi en V7
--                  pero sin prefijo; los dejamos como estan para no romper
--                  compatibilidad. Los nuevos van sin prefijo igual.
--   feature.*    - flags binarios para activar/desactivar features (BOOL).
--                  Cuando la flag esta en 'false', la app oculta el feature
--                  (el boton no aparece, la pantalla no se navega, etc).
--                  El backend ademas valida server-side: aunque la app vieja
--                  pegue al endpoint, si la flag esta apagada, devuelve 403.
--
-- client_visible = true: la app necesita conocer todas las feature.* para
-- gatear su UI. Sin client_visible la app no las recibe.
--
-- Estado inicial:
--   - foto_evidencia / gps_autocompletar / mapa_repartidor / webpay_real / tolerancia_visible:
--     true (estan implementadas y queremos que esten activas).
--   - chat: false (todavia no se implemento; se prende cuando este probado).
--
-- Para flippear: UPDATE app_config SET value='false' WHERE key='feature.chat'.
-- El cache se refresca cada 60s; el efecto se ve en max 60s sin redeploy.

INSERT INTO app_config (key, value, type, description, client_visible) VALUES
    ('feature.foto_evidencia',
     'true', 'BOOL',
     'Picker puede sacar foto de cada item del pedido; cliente la ve en tracking.',
     true),
    ('feature.gps_autocompletar',
     'true', 'BOOL',
     'Cliente puede usar GPS para autocompletar direccion en checkout.',
     true),
    ('feature.mapa_repartidor',
     'true', 'BOOL',
     'Cliente ve mapa con ubicacion del repartidor mientras esta EN_DESPACHO.',
     true),
    ('feature.webpay_real',
     'true', 'BOOL',
     'Pago real con tarjeta via Webpay (vs flujo fake-pay legacy).',
     true),
    ('feature.tolerancia_visible',
     'true', 'BOOL',
     'Muestra el % de tolerancia de peso al cliente en checkout y al picker.',
     true),
    ('feature.chat',
     'false', 'BOOL',
     'Chat in-app cliente <-> picker <-> repartidor. Pendiente de implementacion.',
     true)
ON CONFLICT (key) DO NOTHING;
