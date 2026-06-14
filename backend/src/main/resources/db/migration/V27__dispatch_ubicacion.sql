-- FrutApp · Tracking de ubicacion del repartidor en tiempo real
--
-- Caso de uso: el cliente abre el tracking de un pedido EN_DESPACHO y ve un
-- mapa con la posicion actual del repartidor moviendose hacia su direccion.
-- Es uno de los wow-moments del demo y el principal diferenciador visual
-- frente a competidores ("¿tu app tiene mapa en vivo?").
--
-- Modelado 1:1 con order: cada pedido tiene a lo sumo UNA ubicacion vigente
-- del repartidor (la ultima que reporto). Si necesitamos historial del
-- recorrido para analisis (calcular km/tiempo/tarifa real), agregamos una
-- tabla separada `dispatch_ubicacion_historial` con append-only. Para
-- mostrar en el cliente solo necesitamos la actual → fila unica por order.
--
-- ON DELETE CASCADE: si se borra el pedido, la ubicacion se va con el.
-- repartidor_id NOT NULL: la ubicacion siempre la reporta un repartidor
-- identificado (audit trail; nunca anonima).
--
-- lat/lng NUMERIC(10, 7): 7 decimales = ~1.1 cm de precision (mas que
-- suficiente; los GPS de celus llegan a ~3-5m en ciudad). El typo NUMERIC
-- evita el redondeo de DOUBLE PRECISION en operaciones aritmeticas.

CREATE TABLE dispatch_ubicacion (
    order_id      UUID           PRIMARY KEY REFERENCES customer_order (id) ON DELETE CASCADE,
    repartidor_id UUID           NOT NULL REFERENCES app_user (id),
    lat           NUMERIC(10, 7) NOT NULL,
    lng           NUMERIC(10, 7) NOT NULL,
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now()
);

COMMENT ON TABLE dispatch_ubicacion IS
    'Ultima ubicacion reportada por el repartidor para un pedido en curso. El cliente la consulta cada N segundos en el tracking para pintar el marker.';
