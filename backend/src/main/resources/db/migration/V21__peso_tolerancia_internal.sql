-- FrutApp · peso_tolerancia_porc es config INTERNA (no la consume el cliente).
-- V18 la marco client_visible=true por error (default copiado de costo_envio/envio_gratis_desde
-- que sí van al GET /v1/config publico). La app no la usa: la tolerancia la decide
-- el backend en complete() y getResumenAjuste. Exponerla al cliente sirve solo
-- para que un atacante sepa cuánto manipular el peso para evitar la aprobación.

UPDATE app_config SET client_visible = false WHERE key = 'peso_tolerancia_porc';
