-- FrutApp · Sustitución de items con preservación del pedido original.
-- Cuando el picker no tiene "Ajo 1 kg" y sustituye por "Cebolla Morada 1 kg",
-- queremos preservar QUE pidió el cliente originalmente (nombre, image_key,
-- monto_estimado quedan intactos) y registrar al lado QUE recibió.
-- Asi en el detalle del pedido el cliente ve "Pediste Ajo · Sustituido por
-- Cebolla Morada" con el precio nuevo.
--
-- monto_final usa el precio del sustituto. item_status pasa a SUSTITUIDO.
-- El product_id original queda intacto para auditoria (no apuntamos al
-- sustituto para no perder la traza historica).

ALTER TABLE order_item ADD COLUMN sustituto_nombre TEXT NULL;
ALTER TABLE order_item ADD COLUMN sustituto_image_key TEXT NULL;
ALTER TABLE order_item ADD COLUMN sustituto_product_id UUID NULL;
