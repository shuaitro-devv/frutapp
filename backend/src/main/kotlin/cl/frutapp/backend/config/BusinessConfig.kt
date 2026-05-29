package cl.frutapp.backend.config

/**
 * Parámetros de negocio AJUSTABLES en un solo lugar.
 *
 * Hoy son constantes en código; mañana el "módulo de configuración" (web admin) los
 * leerá desde BD y solo cambiará el ORIGEN de estos valores —el resto de la lógica
 * sigue pidiéndolos acá y no se entera. Toda regla con un "número mágico" ajustable
 * (envío, tasa de FrutCoins, topes, medios/modalidades habilitados) debe vivir aquí.
 */
object BusinessConfig {

    // --- Envío ---
    /** Desde este subtotal (CLP) el envío es gratis. */
    const val ENVIO_GRATIS_DESDE = 15_000
    /** Costo de envío (CLP) bajo el umbral. El retiro en sucursal nunca paga envío. */
    const val COSTO_ENVIO = 2_990

    // --- FrutCoins ---
    /** Se gana 1 FrutCoin por cada N CLP gastados. */
    const val FRUTCOINS_GANA_CADA_CLP = 100
    /** Valor de 1 FrutCoin al pagar (CLP). */
    const val FRUTCOIN_VALOR_CLP = 1
    /** Tope: porcentaje máximo del total que se puede pagar con FrutCoins. */
    const val FRUTCOINS_MAX_PORC_PAGO = 0.20

    // --- Catálogos habilitados (referencia para validación; el front debería pedirlos) ---
    val MEDIOS_PAGO = listOf("TARJETA", "DEBITO", "WEBPAY", "MERCADO_PAGO", "EFECTIVO", "FRUTCOINS", "TRANSFERENCIA")
    val MODALIDADES = listOf("DELIVERY", "RETIRO")

    /** FrutCoins ganadas por un total de compra (CLP). */
    fun frutcoinsPorCompra(totalClp: Int): Int = totalClp / FRUTCOINS_GANA_CADA_CLP

    /** Máximo CLP pagable con FrutCoins para un total dado. */
    fun maxFrutcoinsClp(totalClp: Int): Int = (totalClp * FRUTCOINS_MAX_PORC_PAGO).toInt()
}
