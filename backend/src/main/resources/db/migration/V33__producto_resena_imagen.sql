-- FrutApp · Resenas con foto opcional
--
-- En V32 cada resena tenia solo estrellas + texto. Ahora el cliente puede
-- adjuntar una foto (estuvo bueno el producto, esta defectuoso, llego en
-- mal estado, etc). La foto es opcional — un rating solo con estrellas
-- sigue siendo valido.
--
-- `image_key` apunta al objeto en MinIO. Convencion del key:
-- `reviews/{product_id}/{resena_id}.{ext}`. La URL presignada se calcula
-- en cada GET (TTL 1h); no la persistimos para no migrar filas al rotar
-- credenciales.

ALTER TABLE producto_resena
    ADD COLUMN image_key TEXT;

COMMENT ON COLUMN producto_resena.image_key IS
    'Key del objeto en el bucket de imagenes (MinIO). NULL para resenas solo-texto.';
