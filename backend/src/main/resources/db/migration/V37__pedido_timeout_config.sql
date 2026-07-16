-- FrutApp · Config para auto-cancel de pedidos CREADO (Webpay abandonado)
--
-- Seed keys en app_config para que un admin las pueda ajustar sin redeploy.
-- BusinessConfig las lee con default = 30 min y 5 min respectivamente.
--
-- Semantica:
--   pedido_timeout_min: cuantos minutos dejamos un pedido en CREADO antes
--     de auto-cancelarlo. Cubre "abri Webpay, no complete, volvi atras".
--   pedido_autocancel_job_every_min: cada cuanto corre el job que escanea
--     y transiciona los expirados. client_visible=false porque son params
--     internos de la app, no del usuario final.

INSERT INTO app_config (key, value, type, description, client_visible) VALUES
    ('pedido_timeout_min', '30', 'INT',
     'Minutos que dejamos un pedido CREADO esperando pago antes de auto-cancelarlo (5-1440).',
     false),
    ('pedido_autocancel_job_every_min', '5', 'INT',
     'Frecuencia (min) con la que el job de auto-cancel escanea CREADO expirados (1-60).',
     false)
ON CONFLICT (key) DO NOTHING;
