package cl.frutapp.app.data

/**
 * UUID v4 tiene 36 chars con guiones en posiciones 8, 13, 18, 23. Permite distinguir
 * un uuid del backend (`27800061-d338-4843-8ac5-a53f6e76d713`) de un slug del
 * DemoCatalog (`tomate`, `palta-hass`) o un numero de pedido legible
 * (`#FRU-2026-404659`) sin depender de `java.util.UUID` (que no esta en commonMain).
 */
fun String.isUuidLike(): Boolean =
    length == 36 && this[8] == '-' && this[13] == '-' && this[18] == '-' && this[23] == '-'
