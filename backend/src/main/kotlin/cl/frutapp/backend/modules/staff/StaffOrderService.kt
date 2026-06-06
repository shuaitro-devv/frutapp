package cl.frutapp.backend.modules.staff

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.EventContext
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.auth.UsersTable
import cl.frutapp.backend.modules.orders.OrderItemsTable
import cl.frutapp.backend.modules.orders.OrderStatusHistoryTable
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.shared.dto.StaffOrderSummaryDto
import cl.frutapp.shared.dto.StaffTakeResult
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Logica de la cola del staff (picker / repartidor) bajo el "Modelo C hibrido":
 *  - free-for-all DENTRO de cada pickup_location,
 *  - asignacion atomica via UPDATE con WHERE assigned_picker_id IS NULL,
 *  - auto-rescate de pedidos atascados (>30min sin completar) sin cron job:
 *    la query de cola incluye EN_PICKING con assigned_at viejo.
 *
 * TODOS los eventos relevantes pasan por [UserEventService] para que queden
 * en el ledger user_event ([[V11]]).
 */
class StaffOrderService(
    private val events: UserEventService
) {

    /**
     * Cola libre del picker: pedidos tomables en su location.
     *  - Status CREADO / PAGADO sin asignar.
     *  - EN_PICKING con assigned_at < now()-30min (auto-rescate de atascos).
     */
    suspend fun colaPicker(pickerId: UUID): List<StaffOrderSummaryDto> = dbQuery {
        val location = pickerHomeLocation(pickerId)
        val rescateThreshold = Clock.System.now().minus(STUCK_THRESHOLD)

        val rows = OrdersTable
            .selectAll()
            .where {
                (OrdersTable.pickupLocationId eq location) and (
                    (
                        (OrdersTable.status inList COLA_LIBRE_STATUSES) and
                        OrdersTable.assignedPickerId.isNull()
                    ) or (
                        (OrdersTable.status eq STATUS_EN_PICKING) and
                        OrdersTable.assignedAt.less(rescateThreshold)
                    )
                )
            }
            .orderBy(OrdersTable.createdAt)
            .limit(50)
            .toList()

        materializeSummaries(rows, pickerId)
    }

    /** Mis pedidos en curso (los que tome y aun no completo). */
    suspend fun enCursoPicker(pickerId: UUID): List<StaffOrderSummaryDto> = dbQuery {
        val rows = OrdersTable
            .selectAll()
            .where {
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq STATUS_EN_PICKING)
            }
            .orderBy(OrdersTable.assignedAt)
            .toList()

        materializeSummaries(rows, pickerId)
    }

    /** Toma N rows de pedidos + pre-fetch de los users en 1 query para evitar N+1. */
    private fun materializeSummaries(rows: List<ResultRow>, currentPickerId: UUID): List<StaffOrderSummaryDto> {
        if (rows.isEmpty()) return emptyList()
        val userIds = rows.map { it[OrdersTable.userId] }.toSet()
        val nombrePorUser: Map<UUID, String> = UsersTable
            .selectAll()
            .where { UsersTable.id inList userIds }
            .associate { it[UsersTable.id] to it[UsersTable.name] }

        val orderIds = rows.map { it[OrdersTable.id] }
        val itemsCountPorOrder: Map<UUID, Int> = OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.orderId inList orderIds }
            .groupBy { it[OrderItemsTable.orderId] }
            .mapValues { it.value.size }

        return rows.map { row ->
            val orderId = row[OrdersTable.id]
            val nombre = (nombrePorUser[row[OrdersTable.userId]] ?: "Cliente").substringBefore(' ')
            StaffOrderSummaryDto(
                id = orderId.toString(),
                numero = row[OrdersTable.numero],
                status = row[OrdersTable.status],
                total = row[OrdersTable.totalFinal] ?: row[OrdersTable.totalEstimado],
                itemsCount = itemsCountPorOrder[orderId] ?: 0,
                createdAt = row[OrdersTable.createdAt].toString(),
                clienteNombre = nombre,
                sector = sectorFromAddress(row[OrdersTable.direccion]),
                assignedAt = row[OrdersTable.assignedAt]?.toString(),
                assignedToMe = row[OrdersTable.assignedPickerId] == currentPickerId
            )
        }
    }

    /**
     * Toma atomica del pedido. UPDATE con guard de "no asignado": si dos pickers
     * tocan a la vez, gana el primero y el segundo recibe ok=false.
     */
    suspend fun take(pickerId: UUID, orderId: UUID, context: EventContext): StaffTakeResult {
        val now = Clock.System.now()
        val rescateThreshold = now.minus(STUCK_THRESHOLD)
        // UN solo dbQuery → lookup home + UPDATE atomico + history en la misma
        // transaccion: si crashea entre medio, todo se rollback. El event log
        // queda afuera porque user_event es ledger paralelo, no parte del estado.
        val ok = dbQuery {
            val location = pickerHomeLocation(pickerId)
            val updated = OrdersTable.update({
                (OrdersTable.id eq orderId) and
                (OrdersTable.pickupLocationId eq location) and (
                    (
                        (OrdersTable.status inList COLA_LIBRE_STATUSES) and
                        OrdersTable.assignedPickerId.isNull()
                    ) or (
                        (OrdersTable.status eq STATUS_EN_PICKING) and
                        OrdersTable.assignedAt.less(rescateThreshold)
                    )
                )
            }) {
                it[assignedPickerId] = pickerId
                it[status] = STATUS_EN_PICKING
                it[assignedAt] = now
                it[updatedAt] = now
            }
            if (updated > 0) {
                recordHistory(orderId, fromStatus = null, toStatus = STATUS_EN_PICKING, actorUserId = pickerId, nota = "picker_take")
                true
            } else false
        }

        if (!ok) return StaffTakeResult(ok = false, motivo = "ya_tomado_o_no_disponible")

        events.logSafely(eventType = "staff.order_taken", userId = pickerId, entityType = "order", entityId = orderId, context = context)
        return StaffTakeResult(ok = true, orderId = orderId.toString())
    }

    /** Devolver pedido a la cola libre (clear de assigned_picker_id, status PAGADO). */
    suspend fun release(pickerId: UUID, orderId: UUID, context: EventContext) {
        val now = Clock.System.now()
        val ok = dbQuery {
            val updated = OrdersTable.update({
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq STATUS_EN_PICKING)
            }) {
                it[assignedPickerId] = null
                it[assignedAt] = null
                it[status] = STATUS_PAGADO
                it[updatedAt] = now
            }
            if (updated > 0) {
                recordHistory(orderId, fromStatus = STATUS_EN_PICKING, toStatus = STATUS_PAGADO, actorUserId = pickerId, nota = "picker_release")
                true
            } else false
        }
        if (!ok) throw ValidationException("Este pedido ya no está asignado a ti.")

        events.logSafely(eventType = "staff.order_released", userId = pickerId, entityType = "order", entityId = orderId, context = context)
    }

    /** Marcar el pedido como STOCK_CONFIRMADO (listo para que lo retire el repartidor). */
    suspend fun complete(pickerId: UUID, orderId: UUID, context: EventContext) {
        val now = Clock.System.now()
        val ok = dbQuery {
            val updated = OrdersTable.update({
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq STATUS_EN_PICKING)
            }) {
                it[status] = STATUS_STOCK_CONFIRMADO
                it[updatedAt] = now
            }
            if (updated > 0) {
                recordHistory(orderId, fromStatus = STATUS_EN_PICKING, toStatus = STATUS_STOCK_CONFIRMADO, actorUserId = pickerId, nota = "picker_complete")
                true
            } else false
        }
        if (!ok) throw ValidationException("Este pedido no está en picking o no es tuyo.")

        events.logSafely(eventType = "staff.order_completed", userId = pickerId, entityType = "order", entityId = orderId, context = context)
    }

    // ---- helpers internos (corren dentro de dbQuery) ----

    private fun pickerHomeLocation(pickerId: UUID): UUID {
        val row = UsersTable
            .selectAll()
            .where { UsersTable.id eq pickerId }
            .singleOrNull()
            ?: throw NotFoundException("Usuario no encontrado.")
        return row[UsersTable.homeLocationId]
            ?: throw ValidationException("Tu cuenta no tiene una location asignada. Pide a tu supervisor que la configure.")
    }

    private fun recordHistory(
        orderId: UUID,
        fromStatus: String?,
        toStatus: String,
        actorUserId: UUID?,
        nota: String?
    ) {
        OrderStatusHistoryTable.insert {
            it[id] = UUID.randomUUID()
            it[OrderStatusHistoryTable.orderId] = orderId
            it[OrderStatusHistoryTable.fromStatus] = fromStatus
            it[OrderStatusHistoryTable.toStatus] = toStatus
            it[actor] = "PICKER"
            it[OrderStatusHistoryTable.actorUserId] = actorUserId
            it[OrderStatusHistoryTable.nota] = nota
            it[createdAt] = Clock.System.now()
        }
    }

    /** Heuristica simple para extraer el sector de la direccion ("Av. X, Comuna" -> "Comuna"). */
    private fun sectorFromAddress(direccion: String): String {
        val ultimoSegmento = direccion.split(",").map { it.trim() }.lastOrNull { it.isNotEmpty() }
        return ultimoSegmento ?: "Santiago"
    }

    companion object {
        const val STATUS_CREADO = "CREADO"
        const val STATUS_PAGADO = "PAGADO"
        const val STATUS_EN_PICKING = "EN_PICKING"
        const val STATUS_STOCK_CONFIRMADO = "STOCK_CONFIRMADO"
        val COLA_LIBRE_STATUSES = listOf(STATUS_CREADO, STATUS_PAGADO)
        val STUCK_THRESHOLD = 30.minutes
    }
}
