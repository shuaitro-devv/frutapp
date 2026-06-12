-- FrutApp · Tolerancia de peso para items por kg (fruta variable).
-- Cuando el picker pesa el item y la diferencia con lo estimado supera este %,
-- el pedido pasa a ESPERANDO_AJUSTE_CLIENTE en lugar de STOCK_CONFIRMADO directo,
-- y se le pide al cliente aprobar/rechazar el ajuste antes de seguir.
-- Bajo este umbral, se cobra el real sin interrumpir (UX fluida en el caso comun).
-- Editable por el operador desde el back office sin redeploy.

INSERT INTO app_config (key, value, type, description, client_visible) VALUES
  ('peso_tolerancia_porc', '0.10', 'DECIMAL',
   'Delta tolerado en peso variable antes de pedir aprobacion (0-1; 0.10 = 10%)',
   true);
