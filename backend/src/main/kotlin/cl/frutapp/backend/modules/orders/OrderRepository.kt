package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.config.BusinessConfig
import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.dto.OrderDto
import cl.frutapp.shared.dto.OrderItemDto
import cl.frutapp.shared.dto.OrderPaymentDto
import cl.frutapp.shared.dto.OrderSummaryDto
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
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
    val lines: List<NewOrderLine>,
    val fulfillmentType: String,
    val sucursal: String?,
    val channel: String?,
    val appVersion: String?,
    val deviceModel: String?,
    val osVersion: String?,
    val locale: String?,
    /** Medio de pago para el remanente en efectivo (no-FrutCoins). */
    val cashMethod: String,
    /** CLP que el cliente quiere pagar con FrutCoins (ya capado por config). */
    val frutcoinsClpRequested: Int
)

class OrderRepository {

    /**
     * Crea el pedido de forma ATÓMICA (una sola transacción): orden + items + pagos +
     * historial (CREADO→PAGADO, pre-autorizado simulado) + asientos del ledger de FrutCoins
     * (canje si paga con coins, y ganancia por la compra).
     */
    suspend fun create(o: NewOrder): UUID = dbQuery {
        val orderId = UUID.randomUUID()
        val now = Clock.System.now()

        // Saldo real de FrutCoins (dentro de la txn): vuelve a capar el pago con coins por el
        // saldo disponible, para nunca gastar más de lo que el usuario tiene.
        val saldoActual = FrutCoinsLedgerTable
            .selectAll().where { FrutCoinsLedgerTable.userId eq o.userId }
            .sumOf { it[FrutCoinsLedgerTable.delta] }
        val frutcoinsClp = minOf(o.frutcoinsClpRequested, saldoActual * BusinessConfig.FRUTCOIN_VALOR_CLP)
            .coerceAtLeast(0)
        val coinsGastados = frutcoinsClp / BusinessConfig.FRUTCOIN_VALOR_CLP
        val cashClp = (o.total - frutcoinsClp).coerceAtLeast(0)

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
            it[frutcoinsCanjeadas] = coinsGastados
            it[fulfillmentType] = o.fulfillmentType
            it[sucursal] = o.sucursal
            it[channel] = o.channel
            it[appVersion] = o.appVersion
            it[deviceModel] = o.deviceModel
            it[osVersion] = o.osVersion
            it[locale] = o.locale
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
        if (frutcoinsClp > 0) insertPayment(orderId, PaymentMethod.FRUTCOINS.name, frutcoinsClp, now)
        if (cashClp > 0) insertPayment(orderId, o.cashMethod, cashClp, now)

        insertHistory(orderId, null, OrderStatus.CREADO, OrderActor.CLIENTE, "Pedido creado")
        insertHistory(orderId, OrderStatus.CREADO, OrderStatus.PAGADO, OrderActor.SISTEMA, "Pago pre-autorizado (simulado)")

        var balance = saldoActual
        if (coinsGastados > 0) {
            balance -= coinsGastados
            FrutCoinsLedgerTable.insert {
                it[id] = UUID.randomUUID()
                it[userId] = o.userId
                it[FrutCoinsLedgerTable.orderId] = orderId
                it[delta] = -coinsGastados
                it[motivo] = "CANJE"
                it[balanceAfter] = balance
                it[createdAt] = now
            }
        }
        if (o.frutcoins > 0) {
            balance += o.frutcoins
            FrutCoinsLedgerTable.insert {
                it[id] = UUID.randomUUID()
                it[userId] = o.userId
                it[FrutCoinsLedgerTable.orderId] = orderId
                it[delta] = o.frutcoins
                it[motivo] = "COMPRA"
                it[balanceAfter] = balance
                it[createdAt] = now
            }
        }
        orderId
    }

    private fun insertPayment(orderId: UUID, method: String, monto: Int, now: kotlinx.datetime.Instant) {
        OrderPaymentsTable.insert {
            it[id] = UUID.randomUUID()
            it[OrderPaymentsTable.orderId] = orderId
            it[OrderPaymentsTable.method] = method
            it[OrderPaymentsTable.monto] = monto
            it[createdAt] = now
        }
    }

    suspend fun findDetail(id: UUID, userId: UUID): OrderDto? = dbQuery {
        val row = OrdersTable
            .selectAll().where { (OrdersTable.id eq id) and (OrdersTable.userId eq userId) and OrdersTable.deletedAt.isNull() }
            .singleOrNull() ?: return@dbQuery null
        toOrderDto(row, itemsOf(id), paymentsOf(id))
    }

    /** Sin filtro de usuario (back office). */
    suspend fun findById(id: UUID): OrderDto? = dbQuery {
        val row = OrdersTable.selectAll().where { OrdersTable.id eq id }.singleOrNull() ?: return@dbQuery null
        toOrderDto(row, itemsOf(id), paymentsOf(id))
    }

    suspend fun listByUser(userId: UUID): List<OrderSummaryDto> = dbQuery {
        OrdersTable
            .selectAll().where { (OrdersTable.userId eq userId) and OrdersTable.deletedAt.isNull() }
            .orderBy(OrdersTable.createdAt to SortOrder.DESC)
            .map { row ->
                val id = row[OrdersTable.id]
                OrderSummaryDto(
                    id = id.toString(),
                    numero = row[OrdersTable.numero],
                    status = row[OrdersTable.status],
                    total = row[OrdersTable.totalFinal] ?: row[OrdersTable.totalEstimado],
                    fecha = row[OrdersTable.createdAt].toString(),
                    itemsCount = OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq id }.count().toInt()
                )
            }
    }

    suspend fun currentStatus(id: UUID): OrderStatus? = dbQuery {
        OrdersTable.selectAll().where { OrdersTable.id eq id }.singleOrNull()
            ?.let { OrderStatus.parse(it[OrdersTable.status]) }
    }

    /** Pedidos activos (no borrados) con su estado, para el auto-avance de demo. */
    suspend fun listActive(): List<Pair<UUID, OrderStatus>> = dbQuery {
        OrdersTable.selectAll().where { OrdersTable.deletedAt.isNull() }
            .mapNotNull { row ->
                val st = OrderStatus.parse(row[OrdersTable.status]) ?: return@mapNotNull null
                row[OrdersTable.id] to st
            }
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
        OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq orderId }.map { toItemDto(it) }

    private fun paymentsOf(orderId: UUID): List<OrderPaymentDto> =
        OrderPaymentsTable.selectAll().where { OrderPaymentsTable.orderId eq orderId }
            .orderBy(OrderPaymentsTable.createdAt to SortOrder.ASC)
            .map { OrderPaymentDto(method = it[OrderPaymentsTable.method], monto = it[OrderPaymentsTable.monto]) }

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

    private fun toOrderDto(r: ResultRow, items: List<OrderItemDto>, payments: List<OrderPaymentDto>) = OrderDto(
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
        items = items,
        fulfillmentType = r[OrdersTable.fulfillmentType],
        sucursal = r[OrdersTable.sucursal],
        payments = payments
    )
}
