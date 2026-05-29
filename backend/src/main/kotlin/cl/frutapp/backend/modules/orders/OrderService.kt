package cl.frutapp.backend.modules.orders

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
    private val frutCoins: FrutCoinsRepository
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
        val envio = if (subtotal == 0 || subtotal >= ENVIO_GRATIS_DESDE) 0 else COSTO_ENVIO
        val total = subtotal + envio
        val frutcoins = total / 100
        val numero = "#FRU-2026-${Random.nextInt(100000, 1000000)}"
        val direccion = req.direccion?.takeIf { it.isNotBlank() } ?: DIRECCION_DEMO

        val id = orders.create(
            NewOrder(numero, userId, direccion, ENTREGA_DEMO, subtotal, envio, total, frutcoins, lines)
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

    /** Avance de estado del back office, validando la máquina de estados. */
    suspend fun transition(idStr: String, req: TransitionRequest): OrderDto {
        val id = parseUuid(idStr, "Id inválido.")
        val from = orders.currentStatus(id) ?: throw NotFoundException("Pedido no encontrado.")
        val to = OrderStatus.parse(req.toStatus) ?: throw ValidationException("Estado inválido: ${req.toStatus}")
        if (!OrderStatus.canTransition(from, to)) {
            throw ValidationException("Transición no permitida: $from → $to")
        }
        // MVP: al confirmar stock se fija el total final (= estimado) y se captura el pago.
        val totalFinal = if (to == OrderStatus.STOCK_CONFIRMADO) {
            orders.findById(id)?.totalEstimado
        } else null
        val payment = when (to) {
            OrderStatus.STOCK_CONFIRMADO -> PaymentStatus.CAPTURADO
            OrderStatus.CANCELADO -> PaymentStatus.REEMBOLSADO
            else -> null
        }
        orders.applyTransition(id, from, to, OrderActor.OPERADOR, req.nota, totalFinal, payment)
        return orders.findById(id) ?: throw NotFoundException("Pedido no encontrado.")
    }

    private fun parseUuid(value: String, msg: String): UUID =
        runCatching { UUID.fromString(value) }.getOrNull() ?: throw ValidationException(msg)

    companion object {
        private const val ENVIO_GRATIS_DESDE = 15000
        private const val COSTO_ENVIO = 2990
        private const val DIRECCION_DEMO = "Av. Siempre Viva 742, Santiago"
        private const val ENTREGA_DEMO = "Hoy 10:00 - 12:00"
    }
}
