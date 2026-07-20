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
/** V0.1.13+: tx que se autorizo en Transbank pero cuyo pedido ya estaba
 *  cancelado al llegar el retorno, y para la cual pedimos refund automatico
 *  exitoso via WebpayClient.refund. Distinto de RECHAZADA (que es rechazo
 *  en la autorizacion) y de ERROR (autorizada + refund fallido pendiente
 *  de reconciliacion manual). */
const val WEBPAY_ESTADO_ANULADA = "ANULADA"
/** Refund pedido a Transbank pero la respuesta se perdio (timeout/red). NO
 *  sabemos si la anulacion se ejecuto — no reintentar sin antes consultar
 *  el status en Transbank; hacerlo directo puede causar doble refund. */
const val WEBPAY_ESTADO_REFUND_AMBIGUO = "REFUND_AMBIGUO"
