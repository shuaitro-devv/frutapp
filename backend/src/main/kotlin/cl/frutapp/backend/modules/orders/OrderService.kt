package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.config.BusinessConfig
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.catalog.CatalogRepository
import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.FrutCoinsBalanceDto
import cl.frutapp.shared.dto.OrderDto
import cl.frutapp.shared.dto.OrderSummaryDto
import cl.frutapp.shared.dto.TransitionRequest
import java.util.UUID
import kotlin.random.Random

/**
 * Lógica de pedidos. TODO el cálculo vive acá (fuente de verdad): re-precia desde el
 * catálogo, calcula envío/total/FrutCoins y valida las transiciones de estado. El front
 * solo manda qué quiere y muestra lo que devuelve.
 */
class OrderService(
    private val orders: OrderRepository,
    private val catalog: CatalogRepository,
    private val frutCoins: FrutCoinsRepository,
    /** Hook opcional: fired tras cada transicion exitosa, fire-and-forget. Null
     *  cuando FCM no esta configurado (sin service account) o en tests. */
    private val onTransitionFired: ((orderId: java.util.UUID, from: OrderStatus, to: OrderStatus) -> Unit)? = null
) {

    suspend fun create(userId: UUID, req: CreateOrderRequest): OrderDto {
        if (req.items.isEmpty()) throw ValidationException("El carrito está vacío.")
        val lines = req.items.map { item ->
            if (item.cantidad <= 0) throw ValidationException("Cantidad inválida.")
            val productId = parseUuid(item.productId, "Producto inválido.")
            val p = catalog.findProduct(productId)
                ?: throw NotFoundException("Producto no encontrado: ${item.productId}")
            val esKg = p.unit == "kg"
            val gramos = if (esKg) (item.gramos ?: 1000) else null
            val montoEstimado =
                if (esKg) (p.priceClp * gramos!! / 1000) * item.cantidad
                else p.priceClp * item.cantidad
            NewOrderLine(
                productId = productId,
                nombre = p.name,
                unidad = p.unit,
                imageKey = p.imageKey,
                precioUnitario = p.priceClp,
                gramos = gramos,
                cantidad = item.cantidad,
                montoEstimado = montoEstimado
            )
        }
        val subtotal = lines.sumOf { it.montoEstimado }

        // Modalidad: retiro en sucursal nunca paga envío.
        val fulfillment = FulfillmentType.parse(req.fulfillmentType ?: FulfillmentType.DELIVERY.name)
            ?: throw ValidationException("Modalidad inválida: ${req.fulfillmentType}")
        val esRetiro = fulfillment == FulfillmentType.RETIRO
        val sucursal = if (esRetiro) (req.sucursal?.takeIf { it.isNotBlank() } ?: SUCURSAL_DEMO) else null
        val envio = when {
            esRetiro -> 0
            subtotal == 0 || subtotal >= BusinessConfig.ENVIO_GRATIS_DESDE -> 0
            else -> BusinessConfig.COSTO_ENVIO
        }
        val total = subtotal + envio
        val frutcoins = BusinessConfig.frutcoinsPorCompra(total)
        val numero = "#FRU-2026-${Random.nextInt(100000, 1000000)}"
        val direccion = if (esRetiro) (sucursal ?: SUCURSAL_DEMO)
            else req.direccion?.takeIf { it.isNotBlank() } ?: DIRECCION_DEMO
        val entrega = if (esRetiro) ENTREGA_RETIRO else ENTREGA_DEMO

        // Pagos: valida los medios; capa el monto en FrutCoins por config (% del total).
        // El backend (repo) lo vuelve a capar por el saldo real dentro de la transacción.
        val pagos = req.payments.orEmpty()
        pagos.forEach { p ->
            if (PaymentMethod.parse(p.method) == null) throw ValidationException("Medio de pago inválido: ${p.method}")
        }
        val frutcoinsClpPedido = pagos
            .filter { it.method == PaymentMethod.FRUTCOINS.name }
            .sumOf { (it.monto ?: 0).coerceAtLeast(0) }
        val frutcoinsClpCapado = minOf(frutcoinsClpPedido, BusinessConfig.maxFrutcoinsClp(total), total)
        val cashMethod = pagos.firstOrNull { it.method != PaymentMethod.FRUTCOINS.name }?.method
            ?: PaymentMethod.TARJETA.name

        val id = orders.create(
            NewOrder(
                numero = numero,
                userId = userId,
                direccion = direccion,
                entrega = entrega,
                subtotal = subtotal,
                envio = envio,
                total = total,
                frutcoins = frutcoins,
                lines = lines,
                fulfillmentType = fulfillment.name,
                sucursal = sucursal,
                channel = req.context?.channel,
                appVersion = req.context?.appVersion,
                deviceModel = req.context?.deviceModel,
                osVersion = req.context?.osVersion,
                locale = req.context?.locale,
                cashMethod = cashMethod,
                frutcoinsClpRequested = frutcoinsClpCapado
            )
        )
        return orders.findDetail(id, userId)
            ?: throw IllegalStateException("Pedido recién creado no encontrado")
    }

    suspend fun list(userId: UUID): List<OrderSummaryDto> = orders.listByUser(userId)

    suspend fun detail(userId: UUID, idStr: String): OrderDto {
        val id = parseUuid(idStr, "Id inválido.")
        return orders.findDetail(id, userId) ?: throw NotFoundException("Pedido no encontrado.")
    }

    suspend fun frutCoinsOf(userId: UUID): FrutCoinsBalanceDto = frutCoins.balanceAndHistory(userId)

    /** Avance de estado del back office, validando la máquina de estados.
     *  [actorUserId] es el UUID del usuario que dispara la transicion (admin/operador
     *  desde el back office), null cuando viene de un job sin user (sistema). */
    suspend fun transition(idStr: String, req: TransitionRequest, actorUserId: UUID? = null): OrderDto {
        val id = parseUuid(idStr, "Id inválido.")
        val from = orders.currentStatus(id) ?: throw NotFoundException("Pedido no encontrado.")
        val to = OrderStatus.parse(req.toStatus) ?: throw ValidationException("Estado inválido: ${req.toStatus}")
        if (!OrderStatus.canTransition(from, to)) {
            throw ValidationException("Transición no permitida: $from → $to")
        }
        applyStep(id, from, to, OrderActor.OPERADOR, actorUserId, req.nota)
        return orders.findById(id) ?: throw NotFoundException("Pedido no encontrado.")
    }

    /**
     * Auto-avance de DEMO: mueve cada pedido activo un paso por el camino feliz
     * (PAGADO → … → ENTREGADO). Real (escribe historial con actor SISTEMA), gated por
     * config; nunca toca las ramas de cancelación/devolución.
     */
    suspend fun autoAdvanceAll() {
        orders.listActive().forEach { (id, from) ->
            val to = OrderStatus.nextHappy(from) ?: return@forEach
            if (OrderStatus.canTransition(from, to)) {
                // Avance del cron de demo: actor SISTEMA, sin actorUserId humano.
                applyStep(id, from, to, OrderActor.SISTEMA, null, "Avance automático (demo)")
            }
        }
    }

    /** Aplica una transición ya validada, con los efectos de negocio (captura/reembolso). */
    private suspend fun applyStep(id: UUID, from: OrderStatus, to: OrderStatus, actor: OrderActor, actorUserId: UUID?, nota: String?) {
        // MVP: al confirmar stock se fija el total final (= estimado) y se captura el pago.
        val totalFinal = if (to == OrderStatus.STOCK_CONFIRMADO) orders.findById(id)?.totalEstimado else null
        val payment = when (to) {
            OrderStatus.STOCK_CONFIRMADO -> PaymentStatus.CAPTURADO
            OrderStatus.CANCELADO -> PaymentStatus.REEMBOLSADO
            else -> null
        }
        orders.applyTransition(id, from, to, actor, actorUserId, nota, totalFinal, payment)
        // Hook FCM al final: el push se dispara DESPUES de que la transicion esta
        // persistida (no antes), asi un push nunca se entrega sin que la BD lo refleje.
        // onTransitionFired es fire-and-forget; cualquier excepcion adentro la traga
        // su propio scope, no bloquea ni revienta esta funcion.
        onTransitionFired?.invoke(id, from, to)
    }

    private fun parseUuid(value: String, msg: String): UUID =
        runCatching { UUID.fromString(value) }.getOrNull() ?: throw ValidationException(msg)

    companion object {
        private const val DIRECCION_DEMO = "Av. Siempre Viva 742, Santiago"
        private const val ENTREGA_DEMO = "Hoy 10:00 - 12:00"
        private const val ENTREGA_RETIRO = "Retiro en sucursal"
        private const val SUCURSAL_DEMO = "Sucursal Lo Valledor"
    }
}
