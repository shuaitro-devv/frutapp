-- FrutApp · permiso para que el back-office vea TODOS los pedidos (no solo los propios).
-- `order:read` (V8) lo tiene hasta el cliente -> no sirve como puerta del panel.
-- Este permiso global es para staff de back office (admin / soporte).

INSERT INTO permission (code, description) VALUES
  ('order:read_all', 'Ver todos los pedidos (back office)')
ON CONFLICT (code) DO NOTHING;

-- OJO: el seed de admin en V8 fue un CROSS JOIN evaluado en ESE momento; NO cubre
-- permisos creados después. Por eso el grant a admin va explícito acá.
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'admin' AND p.code = 'order:read_all'
  ON CONFLICT DO NOTHING;

-- soporte: rol de operación/soporte, también ve todos los pedidos.
INSERT INTO role_permission (role_id, permission_id)
  SELECT r.id, p.id FROM role r, permission p
  WHERE r.code = 'soporte' AND p.code = 'order:read_all'
  ON CONFLICT DO NOTHING;
