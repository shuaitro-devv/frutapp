package cl.frutapp.backend.modules.pagos

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Mapa token de Webpay -> pedido + estado de la transaccion (V26).
 * Ver migration para la semantica de `estado` (INICIADA/PAGADA/RECHAZADA/ERROR).
 */
internal object WebpayTransaccionTable : Table("webpay_transaccion") {
    val token = text("token")
    val orderId = uuid("order_id")
    val userId = uuid("user_id")
    val buyOrder = text("buy_order")
    val monto = integer("monto")
    val estado = text("estado")
    val creadoEn = timestamp("creado_en")

    override val primaryKey = PrimaryKey(token)
}

const val WEBPAY_ESTADO_INICIADA = "INICIADA"
const val WEBPAY_ESTADO_PAGADA = "PAGADA"
const val WEBPAY_ESTADO_RECHAZADA = "RECHAZADA"
const val WEBPAY_ESTADO_ERROR = "ERROR"
