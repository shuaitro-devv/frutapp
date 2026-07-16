-- FrutApp · RBAC para el panel de Inteligencia de Mercado (frutapp-admin).
-- Rol "Analista de Mercado" + permiso de lectura del panel. Un sponsor/analista entra
-- SOLO al dashboard de mercado (read-only), sin ser admin.
-- ADITIVO e idempotente: no toca DTOs ni quita nada -> NO rompe el APK desplegado.
-- (El rol nuevo es invisible para los usuarios actuales hasta que se les asigne.)

INSERT INTO permission (code, description) VALUES
  ('market:read', 'Ver Inteligencia de Mercado (back office)')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role (code, name) VALUES
  ('analista', 'Analista de Mercado')
ON CONFLICT (code) DO NOTHING;

-- analista -> market:read
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'analista' AND p.code = 'market:read'
  ON CONFLICT DO NOTHING;

-- admin -> market:read (explícito; el CROSS JOIN de V8 no cubre permisos creados después).
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'admin' AND p.code = 'market:read'
  ON CONFLICT DO NOTHING;
