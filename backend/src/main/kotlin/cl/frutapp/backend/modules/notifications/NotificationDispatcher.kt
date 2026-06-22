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
    private val inbox: NotificationInboxRepository,
    private val fcm: FcmSender?
) {
    private val logger = LoggerFactory.getLogger("NotificationDispatcher")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun onOrderTransition(orderId: UUID, from: OrderStatus, to: OrderStatus) {
        // Cuando un pedido pasa a STOCK_CONFIRMADO (sea desde el complete del picker
        // o desde aprobar/rechazar ajuste del cliente), avisamos a los repartidores
        // de la location. Antes solo lo hacia StaffOrderService.complete, y los
        // pedidos que pasaban por ESPERANDO_AJUSTE_CLIENTE quedaban invisibles
        // para la cola de despacho.
        if (to == OrderStatus.STOCK_CONFIRMADO) {
            scope.launch {
                runCatching {
                    val info = orderRepo.findNumeroAndLocation(orderId) ?: return@launch
                    val (numero, locId) = info
                    if (locId != null) onDispatchReadyForRepartidores(orderId, locId, numero)
                }
            }
        }
        if (fcm == null) return
        // Filtro: solo pushes al cliente para transiciones que le interesan.
        val mensaje = clientMessageFor(to) ?: return
        scope.launch {
            runCatching {
                val ownerInfo = orderRepo.findOwnerAndNumero(orderId) ?: return@launch
                val (customerId, numero) = ownerInfo
                val tituloFinal = mensaje.title
                val cuerpoFinal = mensaje.body.replace("{numero}", numero)

                // 1) Persistir en el inbox ANTES de enviar el push: asi la
                //    pantalla Notificaciones de la app la muestra aunque el
                //    FCM falle o el device no este registrado.
                inbox.create(
                    userId = customerId,
                    type = "PEDIDO",
                    title = tituloFinal,
                    body = cuerpoFinal,
                    data = """{"orderId":"$orderId","orderNumero":"$numero","status":"${to.name}"}"""
                )

                // 2) Enviar push.
                val tokens = deviceTokens.listByUser(customerId)
                if (tokens.isEmpty()) {
                    logger.debug("Sin tokens FCM para user {} (pedido {})", customerId, numero)
                    return@launch
                }
                tokens.forEach { row ->
                    val msg = FcmMessage(
                        token = row.fcmToken,
                        title = tituloFinal,
                        body = cuerpoFinal,
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

    /**
     * Notifica a pickers de una [locationId] que hay un pedido nuevo en cola.
     * Disparado desde `OrderService.create` tras persistir el pedido.
     */
    fun onOrderReadyForPickers(orderId: java.util.UUID, locationId: java.util.UUID, numero: String) {
        scope.launch {
            runCatching {
                val titulo = "Nuevo pedido en cola"
                val cuerpo = "Pedido $numero llegó a tu cola — está esperando un Seleccionador."
                val tokens = deviceTokens.listTokensByRoleInLocation("picker", locationId)
                if (tokens.isEmpty()) return@launch
                // 1) Inbox por cada picker unico (un user puede tener varios tokens).
                tokens.map { it.userId }.toSet().forEach { uid ->
                    inbox.create(
                        userId = uid,
                        type = "PEDIDO",
                        title = titulo,
                        body = cuerpo,
                        data = """{"orderId":"$orderId","orderNumero":"$numero","scope":"picker"}"""
                    )
                }
                // 2) Push FCM (si esta habilitado).
                if (fcm == null) return@launch
                tokens.forEach { row ->
                    val msg = FcmMessage(
                        token = row.fcmToken,
                        title = titulo,
                        body = cuerpo,
                        data = mapOf("type" to "picker_new_order", "orderId" to orderId.toString(), "orderNumero" to numero),
                        androidCollapseKey = "picker_new:$orderId",
                        androidChannelId = ANDROID_CHANNEL_STAFF
                    )
                    when (fcm.send(msg)) {
                        is SendResult.UnregisteredToken -> deviceTokens.deleteByToken(row.fcmToken)
                        else -> Unit
                    }
                }
            }.onFailure { e ->
                logger.error("Error notificando pickers de location {} (pedido {})", locationId, numero, e)
            }
        }
    }

    /**
     * Notifica al PICKER asignado que el cliente resolvio el ajuste de peso.
     * Cierra el loop del trabajo del picker: aprobo (pedido confirmado, va al
     * despacho) o rechazo (algunos items con cambio se descartaron). Hoy llega
     * al inbox + push; el picker no necesita actuar, pero entiende como termino
     * lo que armo. Disparado desde OrderService.aprobarAjuste/rechazarAjuste.
     */
    fun onAjusteResueltoByCliente(orderId: java.util.UUID, aprobado: Boolean) {
        scope.launch {
            runCatching {
                val info = orderRepo.findAssignedPickerAndNumero(orderId) ?: return@launch
                val (pickerId, numero) = info
                val titulo = if (aprobado) "Tu cliente aprobó el ajuste" else "Tu cliente ajustó el pedido"
                val cuerpo = if (aprobado) {
                    "Pedido $numero: el cliente aceptó el ajuste de peso. Ya pasó al repartidor."
                } else {
                    "Pedido $numero: el cliente descartó algunos items con cambio. El resto sigue su curso."
                }
                inbox.create(
                    userId = pickerId,
                    type = "PEDIDO",
                    title = titulo,
                    body = cuerpo,
                    data = """{"orderId":"$orderId","orderNumero":"$numero","scope":"picker","ajuste":"${if (aprobado) "aprobado" else "rechazado"}"}"""
                )
                if (fcm == null) return@launch
                val tokens = deviceTokens.listByUser(pickerId)
                tokens.forEach { row ->
                    val msg = FcmMessage(
                        token = row.fcmToken,
                        title = titulo,
                        body = cuerpo,
                        data = mapOf(
                            "type" to "picker_ajuste_resuelto",
                            "orderId" to orderId.toString(),
                            "orderNumero" to numero,
                            "ajuste" to if (aprobado) "aprobado" else "rechazado"
                        ),
                        androidCollapseKey = "picker_ajuste:$orderId",
                        androidChannelId = ANDROID_CHANNEL_STAFF
                    )
                    when (fcm.send(msg)) {
                        is SendResult.UnregisteredToken -> deviceTokens.deleteByToken(row.fcmToken)
                        else -> Unit
                    }
                }
            }.onFailure { e ->
                logger.error("Error notificando picker del ajuste resuelto del pedido {}", orderId, e)
            }
        }
    }

    /**
     * Notifica a repartidores de una [locationId] que hay un despacho listo
     * para retiro. Disparado desde `StaffOrderService.complete` cuando un
     * pedido pasa a STOCK_CONFIRMADO.
     */
    fun onDispatchReadyForRepartidores(orderId: java.util.UUID, locationId: java.util.UUID, numero: String) {
        scope.launch {
            runCatching {
                val titulo = "Despacho listo para retiro"
                val cuerpo = "Pedido $numero está listo. Pasa a buscarlo cuando puedas."
                val tokens = deviceTokens.listTokensByRoleInLocation("repartidor", locationId)
                if (tokens.isEmpty()) return@launch
                tokens.map { it.userId }.toSet().forEach { uid ->
                    inbox.create(
                        userId = uid,
                        type = "PEDIDO",
                        title = titulo,
                        body = cuerpo,
                        data = """{"orderId":"$orderId","orderNumero":"$numero","scope":"repartidor"}"""
                    )
                }
                if (fcm == null) return@launch
                tokens.forEach { row ->
                    val msg = FcmMessage(
                        token = row.fcmToken,
                        title = titulo,
                        body = cuerpo,
                        data = mapOf("type" to "repartidor_new_dispatch", "orderId" to orderId.toString(), "orderNumero" to numero),
                        androidCollapseKey = "repartidor_new:$orderId",
                        androidChannelId = ANDROID_CHANNEL_STAFF
                    )
                    when (fcm.send(msg)) {
                        is SendResult.UnregisteredToken -> deviceTokens.deleteByToken(row.fcmToken)
                        else -> Unit
                    }
                }
            }.onFailure { e ->
                logger.error("Error notificando repartidores de location {} (pedido {})", locationId, numero, e)
            }
        }
    }

    /**
     * Chat in-app. SIEMPRE persiste en el inbox del destinatario (para que
     * aparezca el badge en la campanita y el item en la pantalla de
     * notificaciones, sin importar si estaba conectado al chat).
     *
     * FCM solo si [destinatarioConectado]=false (si estaba viendo el chat,
     * el mensaje le llego por WS y el push duplicaria el aviso).
     */
    fun onChatMensaje(
        orderId: java.util.UUID,
        destinatarioUserId: java.util.UUID,
        autorRol: String,
        cuerpoBreve: String,
        destinatarioConectado: Boolean,
    ) {
        scope.launch {
            runCatching {
                val tituloAutor = when (autorRol) {
                    "cliente" -> "Cliente"
                    "picker" -> "Seleccionador de Frescura"
                    "repartidor" -> "Repartidor"
                    else -> "Equipo FrutApp"
                }
                val titulo = "Mensaje de tu $tituloAutor"
                inbox.create(
                    userId = destinatarioUserId,
                    type = "CHAT",
                    title = titulo,
                    body = cuerpoBreve,
                    data = """{"orderId":"$orderId","type":"chat_mensaje","autorRol":"$autorRol"}"""
                )
                if (destinatarioConectado) return@launch
                if (fcm == null) return@launch
                val tokens = deviceTokens.listByUser(destinatarioUserId)
                tokens.forEach { row ->
                    val msg = FcmMessage(
                        token = row.fcmToken,
                        title = titulo,
                        body = cuerpoBreve,
                        data = mapOf(
                            "type" to "chat_mensaje",
                            "orderId" to orderId.toString(),
                            "autorRol" to autorRol,
                        ),
                        // Un solo push activo por pedido: notis nuevas pisan
                        // a las anteriores para evitar acumular avisos.
                        androidCollapseKey = "chat:$orderId",
                        androidChannelId = ANDROID_CHANNEL_ORDERS,
                    )
                    when (fcm.send(msg)) {
                        is SendResult.UnregisteredToken -> deviceTokens.deleteByToken(row.fcmToken)
                        else -> Unit
                    }
                }
            }.onFailure { e ->
                logger.error("Error notificando chat del pedido {}", orderId, e)
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
            body = "Pedido {numero}: confirmamos lo que tienes, va camino a tu mesa."
        )
        OrderStatus.ESPERANDO_AJUSTE_CLIENTE -> ClientMessage(
            title = "Tu pedido tiene un ajuste de peso",
            body = "Pedido {numero}: hay items con peso diferente al pedido. Revísalo y aprueba el nuevo total."
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
        /** Canal de notificaciones para estado de pedidos (cara cliente). */
        const val ANDROID_CHANNEL_ORDERS = "frutapp_orders"
        /** Canal para staff (picker / repartidor): nuevo pedido / despacho listo. */
        const val ANDROID_CHANNEL_STAFF = "frutapp_staff"
    }
}
