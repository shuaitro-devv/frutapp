-- FrutApp · RBAC data-driven: roles y permisos como DATOS (no if hardcodeados).
-- Agregar/cambiar un rol o permiso = seed/migración, no código.

CREATE TABLE role (
    id   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL
);

CREATE TABLE permission (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        TEXT NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE role_permission (
    role_id       UUID NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES role (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Roles
INSERT INTO role (code, name) VALUES
  ('cliente', 'Cliente'),
  ('picker', 'Picker'),
  ('repartidor', 'Repartidor'),
  ('proveedor', 'Proveedor'),
  ('soporte', 'Soporte'),
  ('admin', 'Administrador');

-- Permisos
INSERT INTO permission (code, description) VALUES
  ('order:read', 'Ver pedidos'),
  ('order:create', 'Crear pedidos'),
  ('order:pick', 'Preparar (picking)'),
  ('order:confirm_stock', 'Confirmar stock'),
  ('order:invoice', 'Facturar'),
  ('order:dispatch', 'Despachar'),
  ('order:deliver', 'Entregar'),
  ('order:cancel', 'Cancelar'),
  ('order:transition', 'Avanzar el estado de pedidos'),
  ('config:read', 'Leer configuración'),
  ('config:write', 'Editar configuración'),
  ('user:read', 'Ver usuarios'),
  ('user:create', 'Crear usuarios'),
  ('user:assign_role', 'Asignar roles');

-- admin -> todos los permisos
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r CROSS JOIN permission p WHERE r.code = 'admin';

-- cliente
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'cliente' AND p.code IN ('order:create', 'order:read');

-- picker
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'picker' AND p.code IN ('order:read', 'order:pick', 'order:confirm_stock', 'order:transition');

-- repartidor
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'repartidor' AND p.code IN ('order:read', 'order:dispatch', 'order:deliver', 'order:transition');

-- soporte
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'soporte' AND p.code IN ('order:read', 'user:read');

-- proveedor (mínimo por ahora; se define con el equipo)
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'proveedor' AND p.code IN ('order:read');

-- Migrar el rol actual (app_user.role) -> user_role. Hoy todos son CUSTOMER; operadores
-- previos (si los hubiera) van a admin (aún no hay operador granular).
INSERT INTO user_role (user_id, role_id)
  SELECT u.id, r.id FROM app_user u JOIN role r
    ON r.code = CASE WHEN upper(coalesce(u.role, 'CUSTOMER')) IN ('ADMIN', 'OPERATOR') THEN 'admin' ELSE 'cliente' END
  ON CONFLICT DO NOTHING;

-- Bootstrap del primer admin (idempotente; no-op si el correo no existe).
INSERT INTO user_role (user_id, role_id)
  SELECT u.id, r.id FROM app_user u, role r
  WHERE u.email = 'seba.huaitro98@gmail.com' AND r.code = 'admin'
  ON CONFLICT DO NOTHING;
