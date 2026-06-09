-- V15: cambiar notification.data de JSONB a TEXT.
--
-- Por que: en V14 declare la columna como JSONB pensando en queries futuras
-- sobre el payload, pero Exposed la maneja como `text` en la tabla object y
-- ningun INSERT tipa el binding a JSONB → falla con
--   ERROR: column "data" is of type jsonb but expression is of type character varying
--
-- Decision: el payload es ~50-200 bytes con orderId/scope/etc; jsonb-en-bd no
-- aporta vs string (no hacemos queries con `->`), y el front lo deserializa con
-- kotlinx en cualquier caso. Mas simple TEXT.

ALTER TABLE notification ALTER COLUMN data TYPE TEXT;
