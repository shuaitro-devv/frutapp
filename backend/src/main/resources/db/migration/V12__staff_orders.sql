-- ============================================================================
-- V12 - Cola del staff (picker / repartidor) + ubicaciones de retiro
-- ============================================================================
-- Soporta el "Modelo C híbrido" decidido en docs/06-sprints/Plan_Sprint_Demo_Sponsor:
--   - Picker empleado FrutApp en bodega central (ej. Lo Valledor), o
--   - Feriante asociado armando desde su puesto.
-- Misma app, distinta `pickup_location`. La asignación es free-for-all DENTRO
-- de cada location, atómica via UPDATE con WHERE assigned_picker_id IS NULL.
-- ============================================================================

-- 1) Ubicaciones donde se arma el pedido.
--    Empieza con 1 fila ("Lo Valledor Centro"). Cuando se sume Sofruco, va otra
--    fila. Cuando hayan puestos feriantes, una fila por puesto.
CREATE TABLE pickup_location (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        TEXT        NOT NULL UNIQUE,    -- 'lo-valledor-centro', 'sofruco-puesto-47'
    name        TEXT        NOT NULL,           -- 'Lo Valledor Centro'
    address     TEXT,
    is_active   BOOLEAN     NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO pickup_location (code, name, address)
VALUES ('lo-valledor-centro', 'Lo Valledor Centro', 'Camino El Bosque 987, Estación Central, Santiago');


-- 2) Cada usuario staff tiene una location "home" (donde opera por defecto).
--    Nullable: clientes y admins no tienen home_location.
ALTER TABLE app_user
    ADD COLUMN home_location_id UUID REFERENCES pickup_location (id);

CREATE INDEX ix_app_user_home_location ON app_user (home_location_id)
    WHERE home_location_id IS NOT NULL;


-- 3) Cada pedido tiene una location donde se arma + a quien esta asignado
--    (picker y repartidor) + cuando se le asigno al picker (para auto-rescate).
ALTER TABLE customer_order
    ADD COLUMN pickup_location_id     UUID REFERENCES pickup_location (id),
    ADD COLUMN assigned_picker_id     UUID REFERENCES app_user (id),
    ADD COLUMN assigned_repartidor_id UUID REFERENCES app_user (id),
    ADD COLUMN assigned_at            TIMESTAMPTZ;

-- Backfill: todos los pedidos existentes van a Lo Valledor Centro (unica
-- location por ahora). Asi la cola no aparece vacia en el demo.
UPDATE customer_order
SET pickup_location_id = (SELECT id FROM pickup_location WHERE code = 'lo-valledor-centro');

-- pickup_location_id queda NULLABLE: pedidos legacy o creados desde flujos
-- que aun no setean la location tienen NULL. El picker solo ve pedidos con
-- location asignada (la query filtra explicitamente), asi que un NULL
-- equivale a "pedido sin location, ignorar en cola". El OrderService nuevo
-- setea la location default al crear; codigo viejo sigue funcionando sin
-- migrar de golpe. Cuando todo el codigo persista location, podemos hacer
-- NOT NULL en una migracion futura.


-- 4) Indices para las queries que va a hacer el endpoint de cola.
--    La query de "cola libre" filtra por (location, status, assigned_picker NULL).
--    Indice parcial: solo pedidos en estados tomables, pesa muchisimo menos
--    que un indice full y es justo lo que se consulta.
CREATE INDEX ix_order_cola_picker
    ON customer_order (pickup_location_id, status, assigned_at)
    WHERE status IN ('CREADO', 'PAGADO', 'EN_PICKING');

CREATE INDEX ix_order_assigned_picker
    ON customer_order (assigned_picker_id)
    WHERE assigned_picker_id IS NOT NULL;

CREATE INDEX ix_order_assigned_repartidor
    ON customer_order (assigned_repartidor_id)
    WHERE assigned_repartidor_id IS NOT NULL;
