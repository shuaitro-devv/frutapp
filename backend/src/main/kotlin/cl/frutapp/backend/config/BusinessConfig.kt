package cl.frutapp.backend.config

/**
 * Parámetros de negocio AJUSTABLES, leídos de [ConfigCache] (tabla `app_config`) con
 * fallback a estos defaults si la clave no existe. Cambiarlos = editar la fila en BD;
 * al refrescar el caché surten efecto **sin redeploy**. Toda regla con un "número mágico"
 * ajustable (envío, tasa/tope de FrutCoins, medios/modalidades) debe pedirlo acá.
 */
object BusinessConfig {

    // Defaults (fallback si la BD no tiene la clave).
    private const val DEF_ENVIO_GRATIS_DESDE = 15_000
    private const val DEF_COSTO_ENVIO = 2_990
    private const val DEF_FRUTCOINS_GANA_CADA_CLP = 100
    private const val DEF_FRUTCOIN_VALOR_CLP = 1
    private const val DEF_FRUTCOINS_MAX_PORC_PAGO = 0.20

    // --- Envío ---
    val ENVIO_GRATIS_DESDE: Int get() = ConfigCache.int("envio_gratis_desde", DEF_ENVIO_GRATIS_DESDE)
    val COSTO_ENVIO: Int get() = ConfigCache.int("costo_envio", DEF_COSTO_ENVIO)

    // --- FrutCoins --- (coerceAtLeast(1): nunca dividir por cero aunque la BD traiga 0)
    val FRUTCOINS_GANA_CADA_CLP: Int get() = ConfigCache.int("frutcoins_gana_cada_clp", DEF_FRUTCOINS_GANA_CADA_CLP).coerceAtLeast(1)
    val FRUTCOIN_VALOR_CLP: Int get() = ConfigCache.int("frutcoin_valor_clp", DEF_FRUTCOIN_VALOR_CLP).coerceAtLeast(1)
    val FRUTCOINS_MAX_PORC_PAGO: Double get() = ConfigCache.double("frutcoins_max_porc_pago", DEF_FRUTCOINS_MAX_PORC_PAGO)

    // --- Catálogos habilitados (estáticos por ahora) ---
    val MEDIOS_PAGO = listOf("TARJETA", "DEBITO", "WEBPAY", "MERCADO_PAGO", "EFECTIVO", "FRUTCOINS", "TRANSFERENCIA")
    val MODALIDADES = listOf("DELIVERY", "RETIRO")

    /** FrutCoins ganadas por un total de compra (CLP). */
    fun frutcoinsPorCompra(totalClp: Int): Int = totalClp / FRUTCOINS_GANA_CADA_CLP

    /** Máximo CLP pagable con FrutCoins para un total dado. */
    fun maxFrutcoinsClp(totalClp: Int): Int = (totalClp * FRUTCOINS_MAX_PORC_PAGO).toInt()
}
