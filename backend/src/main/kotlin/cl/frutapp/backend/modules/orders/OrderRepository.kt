package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.dto.OrderDto
import cl.frutapp.shared.dto.OrderItemDto
import cl.frutapp.shared.dto.OrderSummaryDto
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update
import java.util.UUID

/** Línea de pedido ya repreciada por el servicio (snapshots + montos). */
data class NewOrderLine(
    val productId: UUID,
    val nombre: String,
    val unidad: String,
    val imageKey: String,
    val precioUnitario: Int,
    val gramos: Int?,
    val cantidad: Int,
    val montoEstimado: Int
)

/** Pedido listo para persistir (totales calculados en el backend). */
data class NewOrder(
    val numero: String,
    val userId: UUID,
    val direccion: String,
    val entrega: String,
    val subtotal: Int,
    val envio: Int,
    val total: Int,
    val frutcoins: Int,
    val lines: List<NewOrderLine>
)

class OrderRepository {

    /**
     * Crea el pedido de forma ATÓMICA (una sola transacción): orden + items + historial
     * (CREADO→PAGADO, pago pre-autorizado simulado) + asiento del ledger de FrutCoins.
     */
    suspend fun create(o: NewOrder): UUID = dbQuery {
        val orderId = UUID.randomUUID()
        val now = Clock.System.now()
        OrdersTable.insert {
            it[id] = orderId
            it[numero] = o.numero
            it[userId] = o.userId
            it[status] = OrderStatus.PAGADO.name
            it[paymentStatus] = PaymentStatus.PREAUTORIZADO.name
            it[direccion] = o.direccion
            it[entrega] = o.entrega
            it[subtotalEstimado] = o.subtotal
            it[envio] = o.envio
            it[totalEstimado] = o.total
            it[frutcoinsGanadas] = o.frutcoins
            it[frutcoinsCanjeadas] = 0
            it[createdAt] = now
            it[updatedAt] = now
        }
        o.lines.forEach { line ->
            OrderItemsTable.insert {
                it[id] = UUID.randomUUID()
                it[OrderItemsTable.orderId] = orderId
                it[productId] = line.productId
                it[nombre] = line.nombre
                it[unidad] = line.unidad
                it[imageKey] = line.imageKey
                it[precioUnitario] = line.precioUnitario
                it[gramos] = line.gramos
                it[cantidad] = line.cantidad
                it[montoEstimado] = line.montoEstimado
                it[itemStatus] = "PENDIENTE"
            }
        }
        insertHistory(orderId, null, OrderStatus.CREADO, OrderActor.CLIENTE, "Pedido creado")
        insertHistory(orderId, OrderStatus.CREADO, OrderStatus.PAGADO, OrderActor.SISTEMA, "Pago pre-autorizado (simulado)")
        if (o.frutcoins > 0) {
            val current = FrutCoinsLedgerTable
                .select { FrutCoinsLedgerTable.userId eq o.userId }
                .sumOf { it[FrutCoinsLedgerTable.delta] }
            FrutCoinsLedgerTable.insert {
                it[id] = UUID.randomUUID()
                it[userId] = o.userId
                it[FrutCoinsLedgerTable.orderId] = orderId
                it[delta] = o.frutcoins
                it[motivo] = "COMPRA"
                it[balanceAfter] = current + o.frutcoins
                it[createdAt] = now
            }
        }
        orderId
    }

    suspend fun findDetail(id: UUID, userId: UUID): OrderDto? = dbQuery {
        val row = OrdersTable
            .select { (OrdersTable.id eq id) and (OrdersTable.userId eq userId) and OrdersTable.deletedAt.isNull() }
            .singleOrNull() ?: return@dbQuery null
        toOrderDto(row, itemsOf(id))
    }

    /** Sin filtro de usuario (back office). */
    suspend fun findById(id: UUID): OrderDto? = dbQuery {
        val row = OrdersTable.select { OrdersTable.id eq id }.singleOrNull() ?: return@dbQuery null
        toOrderDto(row, itemsOf(id))
    }

    suspend fun listByUser(userId: UUID): List<OrderSummaryDto> = dbQuery {
        OrdersTable
            .select { (OrdersTable.userId eq userId) and OrdersTable.deletedAt.isNull() }
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .map { row ->
                val id = row[OrdersTable.id]
                OrderSummaryDto(
                    id = id.toString(),
                    numero = row[OrdersTable.numero],
                    status = row[OrdersTable.status],
                    total = row[OrdersTable.totalFinal] ?: row[OrdersTable.totalEstimado],
                    fecha = row[OrdersTable.createdAt].toString(),
                    itemsCount = OrderItemsTable.select { OrderItemsTable.orderId eq id }.count().toInt()
                )
            }
    }

    suspend fun currentStatus(id: UUID): OrderStatus? = dbQuery {
        OrdersTable.select { OrdersTable.id eq id }.singleOrNull()
            ?.let { OrderStatus.parse(it[OrdersTable.status]) }
    }

    suspend fun applyTransition(
        id: UUID,
        from: OrderStatus,
        to: OrderStatus,
        actor: OrderActor,
        nota: String?,
        totalFinal: Int?,
        paymentStatus: PaymentStatus?
    ) = dbQuery {
        OrdersTable.update({ OrdersTable.id eq id }) {
            it[status] = to.name
            it[updatedAt] = Clock.System.now()
            if (totalFinal != null) it[OrdersTable.totalFinal] = totalFinal
            if (paymentStatus != null) it[OrdersTable.paymentStatus] = paymentStatus.name
        }
        insertHistory(id, from, to, actor, nota)
        Unit
    }

    private fun insertHistory(orderId: UUID, from: OrderStatus?, to: OrderStatus, actor: OrderActor, nota: String?) {
        OrderStatusHistoryTable.insert {
            it[id] = UUID.randomUUID()
            it[OrderStatusHistoryTable.orderId] = orderId
            it[fromStatus] = from?.name
            it[toStatus] = to.name
            it[OrderStatusHistoryTable.actor] = actor.name
            it[OrderStatusHistoryTable.nota] = nota
            it[createdAt] = Clock.System.now()
        }
    }

    private fun itemsOf(orderId: UUID): List<OrderItemDto> =
        OrderItemsTable.select { OrderItemsTable.orderId eq orderId }.map { toItemDto(it) }

    private fun toItemDto(r: ResultRow) = OrderItemDto(
        nombre = r[OrderItemsTable.nombre],
        unidad = r[OrderItemsTable.unidad],
        imageKey = r[OrderItemsTable.imageKey],
        precioUnitario = r[OrderItemsTable.precioUnitario],
        gramos = r[OrderItemsTable.gramos],
        cantidad = r[OrderItemsTable.cantidad],
        montoEstimado = r[OrderItemsTable.montoEstimado],
        montoFinal = r[OrderItemsTable.montoFinal],
        itemStatus = r[OrderItemsTable.itemStatus]
    )

    private fun toOrderDto(r: ResultRow, items: List<OrderItemDto>) = OrderDto(
        id = r[OrdersTable.id].toString(),
        numero = r[OrdersTable.numero],
        status = r[OrdersTable.status],
        paymentStatus = r[OrdersTable.paymentStatus],
        direccion = r[OrdersTable.direccion],
        entrega = r[OrdersTable.entrega],
        subtotalEstimado = r[OrdersTable.subtotalEstimado],
        envio = r[OrdersTable.envio],
        totalEstimado = r[OrdersTable.totalEstimado],
        totalFinal = r[OrdersTable.totalFinal],
        frutcoinsGanadas = r[OrdersTable.frutcoinsGanadas],
        createdAt = r[OrdersTable.createdAt].toString(),
        items = items
    )
}
