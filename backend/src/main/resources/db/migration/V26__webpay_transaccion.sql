-- FrutApp · Webpay Plus (Transbank): tabla de transacciones pendientes
--
-- El flujo de Webpay implica un redirect del browser al backend SIN sesion
-- (la WebView abre una pagina de Transbank, el usuario paga, Transbank
-- redirecciona a nuestro `return_url` con `token_ws`). El backend necesita
-- saber QUE PEDIDO corresponde a ese token. Esta tabla es ese mapa.
--
-- `token` es el lookup key del retorno (siempre llega por query o body POST).
-- Lo emite Transbank al crear la transaccion; es unique y suficientemente
-- aleatorio para no necesitar otro identificador.
--
-- `estado`:
--   INICIADA   - se llamo a Transbank.create; el usuario esta pagando en
--                la WebView. Una INICIADA reciente bloquea iniciar otra
--                para el mismo pedido (anti doble-cobro accidental).
--   PAGADA     - confirmar() devolvio aprobada Y registrarPago() escribio
--                el cambio en customer_order. Estado final feliz.
--   RECHAZADA  - confirmar() devolvio status distinto a AUTHORIZED, o
--                response_code != 0. Estado final.
--   ERROR      - confirmar() exploto, o aprobo pero registrarPago fallo
--                (no por "ya pagado" — eso es PAGADA via ON CONFLICT).
--                Estado final que SIGNIFICA "hay que reconciliar a mano".
--
-- buy_order es el ID que vemos en el panel de Transbank (max 26 chars).
-- Lo guardamos para conciliacion contable y trazabilidad cuando soporte
-- llame "che, esta tx aparece pero el pedido X no esta pagado".
--
-- monto guardado AL INICIAR para comparar al confirmar contra el monto
-- que devuelve Webpay (anti-manipulacion). Si difieren → ERROR.

CREATE TABLE webpay_transaccion (
    token        TEXT        PRIMARY KEY,
    order_id     UUID        NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES app_user (id),
    buy_order    TEXT        NOT NULL,
    monto        INTEGER     NOT NULL,
    estado       TEXT        NOT NULL DEFAULT 'INICIADA',
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index para el guard anti doble-cobro: buscar "tx INICIADA reciente para
-- este pedido" en O(1).
CREATE INDEX ix_webpay_tx_order_estado
    ON webpay_transaccion (order_id, estado, creado_en DESC);

COMMENT ON TABLE webpay_transaccion IS
    'Mapeo token Webpay -> pedido. El retorno publico no trae sesion; el token es el lookup key.';
