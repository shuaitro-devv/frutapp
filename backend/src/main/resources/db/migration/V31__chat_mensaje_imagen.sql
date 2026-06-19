-- FrutApp · Chat: soporte de imagenes adjuntas en mensajes
--
-- En V30 cada mensaje tenia solo `cuerpo` (texto NOT NULL). Ahora un mensaje
-- puede ser:
--   - solo texto       (cuerpo='Hola', image_key=NULL)
--   - texto + imagen   (cuerpo='Mira esta palta', image_key='chat/.../msg.jpg')
--   - solo imagen      (cuerpo='', image_key='chat/.../msg.jpg')
--
-- `image_key` apunta al objeto en MinIO (mismo bucket que evidencia/avatares).
-- La URL presignada se calcula al leer; no la persistimos (rotamos credenciales
-- sin migrar filas). Convencion del key: `chat/{order_id}/{mensaje_id}.{ext}`.
--
-- Cuerpo pasa a aceptar string vacio (mensaje solo-imagen). El check garantiza
-- que SIEMPRE haya contenido: o texto no vacio, o una imagen referenciada.
-- Sin esto un mensaje (cuerpo='', image_key=NULL) seria una burbuja vacia.

ALTER TABLE chat_mensaje
    ADD COLUMN image_key TEXT;

ALTER TABLE chat_mensaje
    ADD CONSTRAINT chk_chat_mensaje_contenido
    CHECK (cuerpo <> '' OR image_key IS NOT NULL);

COMMENT ON COLUMN chat_mensaje.image_key IS
    'Key del objeto en el bucket de imagenes (MinIO). NULL para mensajes solo-texto.';
