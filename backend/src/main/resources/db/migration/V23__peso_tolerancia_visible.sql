-- FrutApp · Revertir V21: peso_tolerancia_porc vuelve a ser client_visible.
--
-- V21 (2026-jun) la oculto al cliente con el argumento "un atacante manipula
-- el peso bajo el umbral para evitar la aprobacion". Ese argumento NO aplica
-- en la practica:
--   - El cliente NO controla el peso; lo pesa el picker (staff).
--   - Si el atacante fuese un picker malicioso, ya tiene acceso al back office
--     y puede leer app_config directo. El "secreto" es trivial de bypassear
--     desde cualquier sesion staff.
--   - El cliente tiene derecho a saber que su peso puede variar +-X% antes de
--     pagar; sin esa info, "te consultamos si cambia mucho" suena arbitrario.
--
-- Volver a client_visible=true habilita que GET /v1/config la incluya y la
-- app la muestre en checkout (cliente) y en la pantalla de pesar (picker,
-- como ayuda de UX, no como dato secreto).

UPDATE app_config SET client_visible = true WHERE key = 'peso_tolerancia_porc';
