package cl.frutapp.backend.modules.notifications

import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Despacha pushes contextuales segun eventos de dominio. Es el unico lugar que sabe
 * QUE texto mostrar para cada transicion; FcmSender solo manda lo que le digan.
 *
 * Estrategia de fire-and-forget: cada evento se procesa en un coroutine propio del
 * scope del dispatcher (no bloquea la request del back office cuando transiciona un
 * pedido). Si FCM no esta configurado (sin service account), [fcm] viene null y los
 * eventos se descartan silenciosamente (logueando un warn al arrancar, no por evento).
 *
 * Si el push falla con UNREGISTERED, el token muerto se borra de [DeviceTokenRepository]
 * para no spamear FCM con tokens fantasma.
 */
class NotificationDispatcher(
    private val orderRepo: OrderRepository,
    private val deviceTokens: DeviceTokenRepository,
    private val fcm: FcmSender?
) {
    private val logger = LoggerFactory.getLogger("NotificationDispatcher")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onOrderTransition(orderId: UUID, from: OrderStatus, to: OrderStatus) {
        if (fcm == null) return
        // Filtro: solo pushes para transiciones que le interesan al cliente.
        val mensaje = clientMessageFor(to) ?: return
        scope.launch {
            runCatching {
                val ownerInfo = orderRepo.findOwnerAndNumero(orderId) ?: return@launch
                val (customerId, numero) = ownerInfo
                val tokens = deviceTokens.listByUser(customerId)
                if (tokens.isEmpty()) {
                    logger.debug("Sin tokens FCM para user {} (pedido {})", customerId, numero)
                    return@launch
                }
                tokens.forEach { row ->
                    val msg = FcmMessage(
                        token = row.fcmToken,
                        title = mensaje.title,
                        body = mensaje.body.replace("{numero}", numero),
                        data = mapOf(
                            "type" to "order_status",
                            "orderId" to orderId.toString(),
                            "orderNumero" to numero,
                            "status" to to.name
                        ),
                        // collapse_key por pedido: si en 10s el pedido pasa por 2
                        // estados, el sistema agrupa y solo muestra el ultimo.
                        androidCollapseKey = "order:$orderId",
                        androidChannelId = ANDROID_CHANNEL_ORDERS
                    )
                    when (fcm.send(msg)) {
                        is SendResult.UnregisteredToken -> {
                            deviceTokens.deleteByToken(row.fcmToken)
                        }
                        else -> Unit
                    }
                }
            }.onFailure { e ->
                logger.error("Error despachando push para pedido {}", orderId, e)
            }
        }
    }

    /** Devuelve el mensaje para el cliente o null si la transicion no le interesa. */
    private fun clientMessageFor(to: OrderStatus): ClientMessage? = when (to) {
        OrderStatus.EN_PICKING -> ClientMessage(
            title = "Tu Seleccionador de Frescura empezó",
            body = "Pedido {numero}: estamos armando tu compra con productos seleccionados."
        )
        OrderStatus.STOCK_CONFIRMADO -> ClientMessage(
            title = "Pedido confirmado",
            body = "Pedido {numero}: confirmamos lo que tenés, va camino a tu mesa."
        )
        OrderStatus.EN_DESPACHO -> ClientMessage(
            title = "Tu Repartidor va camino",
            body = "Pedido {numero}: el repartidor lleva tu pedido a tu mesa."
        )
        OrderStatus.ENTREGADO -> ClientMessage(
            title = "¡Pedido entregado!",
            body = "Pedido {numero}: que lo disfrutes. Gracias por preferirnos."
        )
        OrderStatus.CANCELADO -> ClientMessage(
            title = "Pedido cancelado",
            body = "Pedido {numero}: se canceló y el pago fue reembolsado."
        )
        // Otros estados (PENDIENTE, PAGADO, FACTURADO) no son user-facing.
        else -> null
    }

    private data class ClientMessage(val title: String, val body: String)

    companion object {
        /** Canal de notificaciones para estado de pedidos. La app debe crearlo con
         *  el mismo id antes de mostrar notifs (Android 8+ exige canal). */
        const val ANDROID_CHANNEL_ORDERS = "frutapp_orders"
    }
}
