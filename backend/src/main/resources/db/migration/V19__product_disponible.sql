-- FrutApp · Disponibilidad operacional del producto (stock diario).
-- Distinto de `active` (soft-delete del catalogo). El operador flipea esto
-- cada dia segun lo que llega del proveedor: si Lo Valledor no tiene zanahoria,
-- queda disponible=false → el cliente la ve "Agotado" en el catalogo, no puede
-- agregarla al carrito, y si ya la tenia en el carrito el create-order rechaza
-- con mensaje claro. Aditivo, default true: la app vieja ve todo disponible.

ALTER TABLE product ADD COLUMN disponible BOOLEAN NOT NULL DEFAULT true;

-- Index parcial para acelerar el listado del catalogo del cliente (solo disponibles
-- y activos). El catalogo NO esconde agotados (los muestra en gris); este index
-- ayuda al filtro de busqueda y al check en create-order.
CREATE INDEX IF NOT EXISTS product_active_disponible_idx
  ON product (category_id, disponible)
  WHERE deleted_at IS NULL AND active = true;

-- Permiso para que el back office pueda flipear disponibilidad.
INSERT INTO permission (code, description) VALUES
  ('catalog:write', 'Editar el catalogo (disponibilidad, precios, info de producto)')
  ON CONFLICT (code) DO NOTHING;

-- admin -> tiene catalog:write (igual que el patron de V8 con el CROSS JOIN).
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r CROSS JOIN permission p
  WHERE r.code = 'admin' AND p.code = 'catalog:write'
  ON CONFLICT DO NOTHING;
