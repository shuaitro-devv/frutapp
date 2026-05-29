-- FrutApp · Configuración de negocio en BD (cambiable sin redeploy).
-- La app/lógica leen estos valores vía caché; editar una fila + esperar el refresh = efecto.
CREATE TABLE app_config (
    key            TEXT PRIMARY KEY,
    value          TEXT        NOT NULL,
    type           TEXT        NOT NULL,            -- INT | DECIMAL | STRING | BOOL
    description    TEXT,
    client_visible BOOLEAN     NOT NULL DEFAULT false,  -- si se expone en GET /v1/config
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO app_config (key, value, type, description, client_visible) VALUES
  ('envio_gratis_desde',      '15000', 'INT',     'Subtotal CLP desde el que el envío es gratis', true),
  ('costo_envio',             '2990',  'INT',     'Costo de envío CLP bajo el umbral',            true),
  ('frutcoins_gana_cada_clp', '100',   'INT',     '1 FrutCoin por cada N CLP gastados',           true),
  ('frutcoin_valor_clp',      '1',     'INT',     'Valor de 1 FrutCoin en CLP al pagar',          true),
  ('frutcoins_max_porc_pago', '0.20',  'DECIMAL', 'Tope del total pagable con FrutCoins (0-1)',   true);
