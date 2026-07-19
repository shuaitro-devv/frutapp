package cl.frutapp.backend.modules.pagos

import cl.frutapp.backend.config.WebpayConfig
import cl.frutapp.backend.error.ConflictException
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.EventContext
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.orders.OrderActor
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderStatus
import cl.frutapp.backend.modules.orders.PaymentStatus
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Orquestador del pago via Webpay Plus. Sigue el playbook reutilizable
 * (`H:\Mi unidad\shuaitro-brain\general\integraciones\webpay-plus-playbook.md`)
 * con TODAS las lecciones de plata aplicadas:
 *
 *  1. Anti doble-cobro al iniciar: si hay tx INICIADA reciente para el pedido,
 *     rechaza con 409 antes de pegarle a Transbank.
 *  2. Anti doble-registro al confirmar: la transicion CREADO -> PAGADO usa
 *     applyTransition que ya valida estado origen (TOCTOU cerrado). Un segundo
 *     retorno con el mismo token recibe el estado actual (PAGADA o ERROR).
 *  3. Validacion de monto: confirmar().amount debe coincidir con el monto que
 *     guardamos al iniciar. Si difieren, el pago NO se registra como aprobado.
 *  4. No afirmar exito sin registrar: si confirmar() aprueba pero la transicion
 *     falla por algo distinto a "ya pagado", la tx queda en ERROR y la
 *     respuesta NO dice exito. El cobro existe en Transbank y soporte tiene
 *     que reconciliar (queda log + buy_order para hacerlo).
 *  5. UX honesta de "procesando": el endpoint /pagos/estado puede devolver
 *     INICIADA si el cliente consulta antes de que confirmarRetorno haya
 *     corrido — la app distingue ese estado de RECHAZADA.
 */
class WebpayPagoService(
    private val client: WebpayClient,
    private val repo: WebpayRepository,
    private val orders: OrderRepository,
    private val events: UserEventService,
    private val cfg: WebpayConfig,
    /** Notifica a los pickers de la location del pedido que hay uno nuevo
     *  listo para tomar. Se dispara DESPUES de confirmar el pago, no al crear:
     *  si la app del cliente crea el pedido y queda esperando Webpay, los
     *  pickers no deben verlo hasta que Transbank apruebe. Null en tests. */
    private val onOrderPaid: ((orderId: UUID, locationId: UUID, numero: String) -> Unit)? = null,
) {
    private val logger = LoggerFactory.getLogger(WebpayPagoService::class.java)

    /**
     * El cliente inicia el pago. Valida que el pedido existe, pertenece al
     * usuario y esta en CREADO (sino ya esta pagado o cancelado). Llama a
     * Transbank.crear y persiste la tx. Devuelve el (token, url) para que la
     * app abra la WebView con form-POST.
     */
    suspend fun iniciar(
        userId: UUID,
        orderId: UUID,
        context: EventContext,
    ): IniciarResult {
        val pedido = orders.findDetail(orderId, userId)
            ?: throw NotFoundException("Pedido no encontrado.")
        if (pedido.status != OrderStatus.CREADO.name) {
            throw ConflictException("Este pedido ya no acepta pagos (estado ${pedido.status}).")
        }
        val monto = pedido.totalFinal ?: pedido.totalEstimado
        if (monto <= 0) throw ValidationException("El pedido no tiene un monto valido para cobrar.")

        // Anti doble-cobro: si hay una tx INICIADA hace menos de 10 min, no
        // arrancamos otra. El usuario podria abrir 2 ventanas y cobrarse 2
        // veces si dejamos crear varias. 10 min cubre la sesion de Webpay
        // estandar (que expira en ~10 min) sin bloquear al usuario que
        // realmente quiere reintentar despues de cerrar la WebView.
        val umbral = Clock.System.now().minus(10.minutes)
        if (repo.hayIniciadaReciente(orderId, umbral)) {
            throw ConflictException("Ya tienes un pago en curso para este pedido. Espera unos minutos antes de reintentar.")
        }

        // buy_order: max 26 chars (limite de Transbank). UUID sin guiones (32
        // chars) NO entra; usamos los primeros 26 — sigue siendo unico para
        // identificar la tx en el panel de Transbank cuando soporte reconcilia.
        val buyOrder = orderId.toString().replace("-", "").take(26)
        val returnUrl = "${cfg.returnUrlBase}/v1/pagos/webpay/retorno"
        val sessionId = userId.toString().take(60)  // limite Transbank

        val crear = try {
            client.crear(
                buyOrder = buyOrder,
                sessionId = sessionId,
                montoClp = monto,
                returnUrl = returnUrl,
            )
        } catch (e: WebpayException) {
            // No registramos tx si Transbank rechazo crear — no hay token.
            logger.warn("Webpay.crear fallo para order=$orderId", e)
            events.logSafely(
                eventType = "pago.webpay_crear_error",
                userId = userId,
                entityType = "order",
                entityId = orderId,
                payload = buildJsonObject {
                    put("error", JsonPrimitive(e.message ?: "unknown"))
                },
                context = context,
            )
            throw ValidationException("No pudimos iniciar el pago. Reintenta en unos minutos.")
        }

        repo.insert(crear.token, orderId, userId, buyOrder, monto)

        events.logSafely(
            eventType = "pago.webpay_iniciado",
            userId = userId,
            entityType = "order",
            entityId = orderId,
            payload = buildJsonObject {
                put("token", JsonPrimitive(crear.token))
                put("buyOrder", JsonPrimitive(buyOrder))
                put("monto", JsonPrimitive(monto))
                put("sandbox", JsonPrimitive(cfg.esSandbox))
            },
            context = context,
        )

        return IniciarResult(token = crear.token, urlFormPost = crear.urlFormPost)
    }

    /**
     * Webpay redirige a `/v1/pagos/webpay/retorno?token_ws=...` (PUBLICO, sin
     * sesion). Hace el commit, registra el pago si aprobo, marca la tx final.
     *
     * Devuelve [RetornoResult] para que el handler HTTP genere un HTML simple
     * que el WebView del cliente lee y cierra.
     *
     * No tira excepciones: errores tecnicos los registra como ERROR y devuelve
     * un resultado "fallida" — el HTML siempre debe responderse OK al browser
     * o Webpay marca el comercio como roto.
     */
    suspend fun confirmarRetorno(token: String): RetornoResult {
        val tx = repo.findByToken(token)
            ?: return RetornoResult.Desconocida

        // Idempotencia: si la tx ya tiene estado final, devolvemos lo que ya
        // sabemos (el browser pudo volver a tocar el retorno con back). NO
        // re-llamamos a Transbank — eso da 422.
        when (tx.estado) {
            WEBPAY_ESTADO_PAGADA -> return RetornoResult.Aprobada
            WEBPAY_ESTADO_RECHAZADA -> return RetornoResult.Rechazada
            WEBPAY_ESTADO_ERROR -> return RetornoResult.Error
        }

        val confirmar = try {
            client.confirmar(token)
        } catch (e: WebpayException) {
            logger.warn("Webpay.confirmar fallo para tx=$token", e)
            repo.cambiarEstado(token, WEBPAY_ESTADO_ERROR)
            return RetornoResult.Error
        }

        if (!confirmar.aprobada) {
            repo.cambiarEstado(token, WEBPAY_ESTADO_RECHAZADA)
            events.logSafely(
                eventType = "pago.webpay_rechazado",
                userId = tx.userId,
                entityType = "order",
                entityId = tx.orderId,
                payload = buildJsonObject {
                    put("token", JsonPrimitive(token))
                    put("status", JsonPrimitive(confirmar.status ?: "?"))
                    put("responseCode", JsonPrimitive(confirmar.responseCode ?: -1))
                },
            )
            return RetornoResult.Rechazada
        }

        // Reconciliar monto: si Webpay reporta un monto distinto al que
        // pusimos al iniciar, NO afirmar exito — alguien manipulo o hubo
        // bug. Marca ERROR y log.
        if (confirmar.monto != tx.monto) {
            logger.error("Webpay.confirmar monto distinto: esperado=${tx.monto} recibido=${confirmar.monto} tx=$token")
            repo.cambiarEstado(token, WEBPAY_ESTADO_ERROR)
            events.logSafely(
                eventType = "pago.webpay_monto_mismatch",
                userId = tx.userId,
                entityType = "order",
                entityId = tx.orderId,
                payload = buildJsonObject {
                    put("token", JsonPrimitive(token))
                    put("esperado", JsonPrimitive(tx.monto))
                    put("recibido", JsonPrimitive(confirmar.monto ?: -1))
                },
            )
            return RetornoResult.Error
        }

        // Aprobado y monto OK: transicion CREADO -> PAGADO atomica (applyTransition
        // cierra el TOCTOU). Si el pedido ya esta PAGADO por una tx previa
        // (concurrencia), ConflictException → marcamos PAGADA igual (es la
        // verdad del mundo) y devolvemos Aprobada.
        try {
            orders.applyTransition(
                id = tx.orderId,
                from = OrderStatus.CREADO,
                to = OrderStatus.PAGADO,
                actor = OrderActor.CLIENTE,
                actorUserId = tx.userId,
                nota = "Pago Webpay (auth=${confirmar.authorizationCode ?: "?"})",
                totalFinal = null,
                paymentStatus = PaymentStatus.CAPTURADO,
            )
            repo.cambiarEstado(token, WEBPAY_ESTADO_PAGADA)
        } catch (e: ConflictException) {
            // ConflictException = la transicion CREADO->PAGADO no encontro un
            // pedido en CREADO. Puede ser:
            //  (a) ya paso a PAGADO por otra tx concurrente → exito.
            //  (b) paso a CANCELADO por el auto-cancel job → CATASTROFE:
            //      Transbank ya cobro pero el pedido esta cancelado. Marcamos
            //      ERROR y dejamos que soporte reconcilie con refund manual.
            val statusActual = orders.currentStatus(tx.orderId)
            when (statusActual) {
                OrderStatus.PAGADO, OrderStatus.EN_PICKING, OrderStatus.STOCK_CONFIRMADO,
                OrderStatus.FACTURADO, OrderStatus.EN_DESPACHO, OrderStatus.ENTREGADO,
                OrderStatus.ESPERANDO_AJUSTE_CLIENTE -> {
                    // (a) ya avanzo por otra ruta — exito.
                    logger.info("Webpay.retorno: pedido ${tx.orderId} ya estaba pagado (concurrencia OK, status=$statusActual)")
                    repo.cambiarEstado(token, WEBPAY_ESTADO_PAGADA)
                }
                else -> {
                    // (b) CANCELADO/DEVOLUCION o null → dinero cobrado sin
                    // pedido. Marcamos ERROR, evento pago.webpay_pedido_cancelado
                    // para alertar soporte, y respondemos Error al cliente.
                    logger.error("Webpay.retorno: pedido ${tx.orderId} en status=$statusActual pero Transbank aprobo — refund manual necesario")
                    repo.cambiarEstado(token, WEBPAY_ESTADO_ERROR)
                    events.logSafely(
                        eventType = "pago.webpay_pedido_cancelado",
                        userId = tx.userId,
                        entityType = "order",
                        entityId = tx.orderId,
                        payload = buildJsonObject {
                            put("token", JsonPrimitive(token))
                            put("authorizationCode", JsonPrimitive(confirmar.authorizationCode ?: "?"))
                            put("buyOrder", JsonPrimitive(tx.buyOrder))
                            put("statusActual", JsonPrimitive(statusActual?.name ?: "null"))
                            put("monto", JsonPrimitive(tx.monto))
                        },
                    )
                    return RetornoResult.Error
                }
            }
        } catch (e: Throwable) {
            // ERROR de verdad: el dinero esta cobrado en Transbank pero no
            // logramos marcar el pedido como pagado. Marcamos ERROR y dejamos
            // que soporte reconcilie con el buy_order + authorization_code.
            logger.error("Webpay.retorno: fallo al marcar pedido ${tx.orderId} como pagado tras aprobacion", e)
            repo.cambiarEstado(token, WEBPAY_ESTADO_ERROR)
            events.logSafely(
                eventType = "pago.webpay_registro_fallo",
                userId = tx.userId,
                entityType = "order",
                entityId = tx.orderId,
                payload = buildJsonObject {
                    put("token", JsonPrimitive(token))
                    put("authorizationCode", JsonPrimitive(confirmar.authorizationCode ?: "?"))
                    put("buyOrder", JsonPrimitive(tx.buyOrder))
                    put("error", JsonPrimitive(e.message ?: "unknown"))
                },
            )
            return RetornoResult.Error
        }

        events.logSafely(
            eventType = "pago.webpay_aprobado",
            userId = tx.userId,
            entityType = "order",
            entityId = tx.orderId,
            payload = buildJsonObject {
                put("token", JsonPrimitive(token))
                put("monto", JsonPrimitive(confirmar.monto ?: 0))
                put("authorizationCode", JsonPrimitive(confirmar.authorizationCode ?: "?"))
                put("ultimosDigitosTarjeta", JsonPrimitive(confirmar.ultimosDigitosTarjeta ?: ""))
            },
        )

        // Notificar pickers: ahora que el pedido esta PAGADO de verdad, los
        // pickers de la location pueden tomarlo. En OrderService.create
        // saltamos este hook para pedidos esperandoWebpay (sino los pickers
        // recibian el push antes de que Transbank confirmara y el flujo
        // arrancaba sin pago real).
        val onPaid = onOrderPaid
        if (onPaid != null) {
            runCatching {
                orders.findNumeroAndLocation(tx.orderId)?.let { (numero, locId) ->
                    if (locId != null) onPaid(tx.orderId, locId, numero)
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) throw e
                logger.warn("Webpay.retorno: fallo al notificar pickers para pedido ${tx.orderId}", e)
            }
        }
        return RetornoResult.Aprobada
    }

    /** El cliente consulta el resultado al cerrar la WebView. La app distingue
     *  INICIADA ("procesando — confirmando con Webpay") de RECHAZADA. */
    suspend fun estado(token: String, userId: UUID): EstadoTx? {
        val tx = repo.findByToken(token) ?: return null
        if (tx.userId != userId) return null  // ownership
        return EstadoTx(token = tx.token, estado = tx.estado, orderId = tx.orderId.toString())
    }
}

data class IniciarResult(val token: String, val urlFormPost: String)

sealed class RetornoResult {
    object Aprobada : RetornoResult()
    object Rechazada : RetornoResult()
    object Error : RetornoResult()
    object Desconocida : RetornoResult()
}

data class EstadoTx(val token: String, val estado: String, val orderId: String)
