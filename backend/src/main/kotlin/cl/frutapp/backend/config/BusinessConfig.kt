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
    private const val DEF_PESO_TOLERANCIA_PORC = 0.10

    // --- Envío ---
    // coerceAtLeast(0): si un admin compromised setea negativo, el costo se clampa
    // a 0 ("gratis") en lugar de regalar plata (cobrar negativo). El umbral de envio
    // gratis con negativo significaria "todo es gratis" — tambien clampamos.
    val ENVIO_GRATIS_DESDE: Int get() = ConfigCache.int("envio_gratis_desde", DEF_ENVIO_GRATIS_DESDE).coerceAtLeast(0)
    val COSTO_ENVIO: Int get() = ConfigCache.int("costo_envio", DEF_COSTO_ENVIO).coerceAtLeast(0)

    // --- FrutCoins --- (coerceAtLeast(1): nunca dividir por cero aunque la BD traiga 0)
    val FRUTCOINS_GANA_CADA_CLP: Int get() = ConfigCache.int("frutcoins_gana_cada_clp", DEF_FRUTCOINS_GANA_CADA_CLP).coerceAtLeast(1)
    val FRUTCOIN_VALOR_CLP: Int get() = ConfigCache.int("frutcoin_valor_clp", DEF_FRUTCOIN_VALOR_CLP).coerceAtLeast(1)
    // coerceIn(0, 0.5): tope absoluto al 50% del total pagable con coins, asi un admin
    // compromised no puede setear 1.5 ("paga 150% con coins") ni negativo. Por encima
    // de 0.5 el incentivo de retornar al ecosistema se rompe (mejor cobrar dinero real).
    val FRUTCOINS_MAX_PORC_PAGO: Double get() = ConfigCache.double("frutcoins_max_porc_pago", DEF_FRUTCOINS_MAX_PORC_PAGO).coerceIn(0.0, 0.5)

    // --- Peso variable (kg) --- (delta tolerado antes de pedir aprobacion al cliente)
    // coerceIn(0, 1): si el operador setea un valor fuera de rango (ej. "5" interpretado
    // como 5% pero el codigo espera 0-1 = 0-100%), clampeamos al maximo razonable para
    // que no se desactive la logica (un 500% nunca dispararia ESPERANDO_AJUSTE y
    // cobrariamos delta sin consultar). 0 desactiva la tolerancia (siempre pregunta).
    val PESO_TOLERANCIA_PORC: Double get() =
        ConfigCache.double("peso_tolerancia_porc", DEF_PESO_TOLERANCIA_PORC).coerceIn(0.0, 1.0)

    // --- Catálogos habilitados (estáticos por ahora) ---
    val MEDIOS_PAGO = listOf("TARJETA", "DEBITO", "WEBPAY", "MERCADO_PAGO", "EFECTIVO", "FRUTCOINS", "TRANSFERENCIA")
    val MODALIDADES = listOf("DELIVERY", "RETIRO")

    /** FrutCoins ganadas por un total de compra (CLP). */
    fun frutcoinsPorCompra(totalClp: Int): Int = totalClp / FRUTCOINS_GANA_CADA_CLP

    /** Máximo CLP pagable con FrutCoins para un total dado. */
    fun maxFrutcoinsClp(totalClp: Int): Int = (totalClp * FRUTCOINS_MAX_PORC_PAGO).toInt()
}
