-- FrutApp · Evidencia visual por item de pedido
--
-- Caso de uso primario: el picker, al completar un item, saca foto del
-- producto en el canasto del cliente (y, opcionalmente, foto a la balanza
-- para items por kg). Eso da:
--   - al CLIENTE: prueba visual de que el pedido se armo y refleja lo que
--     pago (gana confianza, reduce reclamos por contenido).
--   - al SOPORTE: evidencia para resolver "lo que llego no era esto".
--   - al NEGOCIO: registro de armado correcto para auditorias post-incidente
--     (producto vencido, golpe, etc).
--
-- Modelado N a 1 (varias filas por order_item) desde el dia 1 aunque la UI
-- MVP solo permita 1 foto: escalar a N solo cambia el frontend, sin tocar
-- backend ni BD. Eso da margen para:
--   - "frente + dorso + etiqueta" (3 fotos al mismo item)
--   - "foto del producto + foto de la balanza" para kg
--   - "foto del cliente reclamando" (mismo modelo, otro uploader)
--
-- comentario nullable: a futuro el picker podra adjuntar nota textual ("la
-- palta venia con un golpe en este lado"). UI MVP no expone el input pero
-- el endpoint lo acepta.
--
-- uploaded_by NOT NULL + FK a app_user: trazabilidad obligatoria (quien
-- subio cada foto, sin posibilidad de evidencia "anonima").
--
-- order_id DENORMALIZADO (no derivado por JOIN): la query "todas las
-- evidencias del pedido X" la corre el cliente al ver el tracking, y debe
-- ser barata. Sin denorm seria order_item_evidence x order_item para llegar
-- al pedido. La denorm se mantiene consistente porque el INSERT siempre
-- resuelve el order_id desde el order_item al crear, y nunca se updatea.

CREATE TABLE order_item_evidence (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_item_id UUID        NOT NULL REFERENCES order_item (id) ON DELETE CASCADE,
    order_id      UUID        NOT NULL REFERENCES customer_order (id) ON DELETE CASCADE,
    image_key     TEXT        NOT NULL,                                -- key en MinIO
    comentario    TEXT,                                                -- nullable, max 500 chars (validado en codigo)
    uploaded_by   UUID        NOT NULL REFERENCES app_user (id),
    uploaded_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indices para las 2 queries esperadas:
--  1. "evidencias de este item" (UI del item en detalle picker/cliente).
--  2. "evidencias de este pedido" (visor del cliente en tracking, agrupado por item).
CREATE INDEX ix_order_item_evidence_item  ON order_item_evidence (order_item_id);
CREATE INDEX ix_order_item_evidence_order ON order_item_evidence (order_id, uploaded_at DESC);

COMMENT ON TABLE order_item_evidence IS
    'Fotos de evidencia que el picker (y a futuro repartidor/cliente) sube de los items de un pedido. N:1 con order_item.';
