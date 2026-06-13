package cl.frutapp.backend.modules.staff

import cl.frutapp.backend.config.BusinessConfig
import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.EventContext
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.auth.UsersTable
import cl.frutapp.backend.modules.notifications.NotificationDispatcher
import cl.frutapp.backend.modules.orders.OrderItemsTable
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderStatus
import cl.frutapp.backend.modules.orders.OrderStatusHistoryTable
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.shared.dto.StaffDispatchDetailDto
import cl.frutapp.shared.dto.StaffDispatchSummaryDto
import cl.frutapp.shared.dto.StaffOrderDetailDto
import cl.frutapp.shared.dto.StaffOrderItemDto
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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.jetbrains.exposed.sql.SortOrder

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
    private val events: UserEventService,
    /** Opcional: dispara push al cliente cuando la transicion de picker/repartidor
     *  avanza el estado del pedido. Null en tests donde no se quiere validar push. */
    private val notifications: NotificationDispatcher? = null,
    /** Resuelve la URL presignada del avatar del cliente para que el repartidor lo
     *  vea en su pantalla de detalle/cola. Null cuando MinIO no esta configurado. */
    private val avatarUrlResolver: (suspend (UUID) -> String?)? = null,
    /** Para los flows de peso variable (setItemPeso, complete con discriminacion
     *  por tolerancia). Usa los helpers tipados del repo en vez de SQL inline para
     *  evitar duplicar logica de calculo. */
    private val orderRepository: OrderRepository = OrderRepository()
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

    /** Detalle de un pedido para el picker: cabecera + items reales. Verifica
     *  permisos de location (no se puede ver un pedido de otra ubicacion). */
    suspend fun detalle(pickerId: UUID, orderId: UUID): StaffOrderDetailDto = dbQuery {
        val pickerLocation = pickerHomeLocation(pickerId)
        val orderRow = OrdersTable
            .selectAll()
            .where { OrdersTable.id eq orderId }
            .singleOrNull()
            ?: throw NotFoundException("Pedido no encontrado.")

        val pedidoLocation = orderRow[OrdersTable.pickupLocationId]
        if (pedidoLocation != pickerLocation) {
            throw ValidationException("Este pedido no es de tu location.")
        }

        val clienteUserId = orderRow[OrdersTable.userId]
        val clienteRow = UsersTable
            .selectAll()
            .where { UsersTable.id eq clienteUserId }
            .singleOrNull()
        val clienteNombre = (clienteRow?.get(UsersTable.name) ?: "Cliente").substringBefore(' ')

        val itemsRows = OrderItemsTable
            .selectAll()
            .where { OrderItemsTable.orderId eq orderId }
            .toList()

        val items = itemsRows.mapIndexed { index, row ->
            val unidad = row[OrderItemsTable.unidad]
            val gramos = row[OrderItemsTable.gramos]
            val esKg = unidad == "kg"
            val cantidadDouble = if (esKg && gramos != null) {
                // cantidad * gramos -> mostramos como kg (ej. 2 x 1000g = 2.0 kg)
                row[OrderItemsTable.cantidad] * gramos / 1000.0
            } else {
                row[OrderItemsTable.cantidad].toDouble()
            }
            StaffOrderItemDto(
                numero = index + 1,
                productId = row[OrderItemsTable.productId].toString(),
                nombre = row[OrderItemsTable.nombre],
                unidad = unidad,
                cantidad = cantidadDouble,
                gramos = gramos,
                precioUnitario = row[OrderItemsTable.precioUnitario],
                montoEstimado = row[OrderItemsTable.montoEstimado],
                pesoVariable = esKg && gramos != null,
                emoji = emojiForProduct(row[OrderItemsTable.nombre]),
                imageKey = row[OrderItemsTable.imageKey],
                id = row[OrderItemsTable.id].toString(),
                pesoReal = row[OrderItemsTable.pesoReal],
                itemStatus = row[OrderItemsTable.itemStatus]
            )
        }

        StaffOrderDetailDto(
            id = orderId.toString(),
            numero = orderRow[OrdersTable.numero],
            status = orderRow[OrdersTable.status],
            total = orderRow[OrdersTable.totalFinal] ?: orderRow[OrdersTable.totalEstimado],
            createdAt = orderRow[OrdersTable.createdAt].toString(),
            clienteNombre = clienteNombre,
            sector = sectorFromAddress(orderRow[OrdersTable.direccion]),
            assignedAt = orderRow[OrdersTable.assignedAt]?.toString(),
            assignedToMe = orderRow[OrdersTable.assignedPickerId] == pickerId,
            items = items
        )
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

    /** Tab "Listos hoy" del picker: pedidos que el picker logueado completo
     *  (status >= STOCK_CONFIRMADO, EN_DESPACHO, ENTREGADO, FACTURADO) en las
     *  ultimas 24h. Excluye CANCELADO/DEVOLUCION para no contaminar el conteo
     *  de productividad. Ordenado del mas reciente al mas antiguo. */
    suspend fun completadosHoyPicker(pickerId: UUID): List<StaffOrderSummaryDto> = dbQuery {
        val ayer = Clock.System.now().minus(24.hours)
        val rows = OrdersTable
            .selectAll()
            .where {
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status inList COMPLETADOS_PICKER_STATUSES) and
                (OrdersTable.updatedAt greater ayer)
            }
            .orderBy(OrdersTable.updatedAt, SortOrder.DESC)
            .limit(50)
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
        // Push al cliente: "Tu Seleccionador empezó". `from` se loguea como PAGADO,
        // pero el dispatcher solo mira el `to` para decidir el copy del push.
        notifications?.onOrderTransition(orderId, OrderStatus.PAGADO, OrderStatus.EN_PICKING)
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

    /**
     * El picker mide el peso real en bascula y lo registra. Valida:
     *  - ownership: el picker tiene el pedido asignado y esta en EN_PICKING.
     *  - el item es por kg (unidad="kg"), si no rechaza (no tiene sentido pesar unidades).
     *  - gramosReales > 0.
     *
     * Calcula `monto_final = precio_unitario * gramos_reales / 1000 * cantidad`,
     * persiste peso_real + monto_final, marca item_status=CONFIRMADO.
     */
    suspend fun setItemPeso(pickerId: UUID, orderId: UUID, itemId: UUID, gramosReales: Int, context: EventContext) {
        if (gramosReales <= 0) throw ValidationException("El peso debe ser mayor a 0.")

        // Lectura del item para validar que es por kg + calcular montoFinal. Esto
        // puede salir desactualizado si el pedido cambia de estado entre acá y el
        // UPDATE — pero el UPDATE de abajo es atomico (chequea ownership + EN_PICKING
        // en la misma dbQuery), asi que el peor caso es que escribimos NADA y
        // devolvemos error (no quedamos en estado inconsistente).
        val items = orderRepository.listItemsPesoInfo(orderId)
        val item = items.firstOrNull { it.id == itemId }
        // Mensaje generico para no filtrar metadata de pedidos ajenos al picker:
        // antes, los strings "Item no encontrado" vs "Este item no se pesa" vs
        // "Pedido no en picking o no tuyo" permitian enumerar si el item existe,
        // su unidad y el ownership del pedido. Devolvemos siempre el mismo error
        // hasta que el UPDATE atomico del repo confirme la operacion.
        if (item == null || item.unidad != "kg") {
            throw ValidationException("No se puede registrar el peso para este item.")
        }

        // Contrato: gramosReales = peso TOTAL del item (todas las bolsas pesadas
        // juntas en bascula). El UI del picker muestra "cantidad * gramos / 1000 kg
        // solicitados" y el picker pesa ese total. monto_final = precio_unitario
        // (CLP/kg) * gramosReales / 1000. NO multiplicar de nuevo por cantidad —
        // la cantidad ya esta absorbida en el peso total. Antes lo hacia y cobraba
        // doble/triple en pedidos con cantidad > 1.
        val montoFinal = (item.precioUnitario.toLong() * gramosReales / 1000L).toInt()
        val updated = orderRepository.setItemPeso(orderId, pickerId, itemId, gramosReales, montoFinal)
        if (updated == 0) throw ValidationException("Este pedido no está en picking o no es tuyo.")

        events.logSafely(
            eventType = "staff.item_peso_set",
            userId = pickerId, entityType = "order", entityId = orderId,
            context = context
        )
    }

    /** Picker sustituye un item por un producto similar disponible. Valida que
     *  el item y el sustituto existan, y que el pedido este EN_PICKING asignado
     *  a este picker. Recalcula monto_final con el precio del sustituto. */
    suspend fun sustituirItem(
        pickerId: UUID,
        orderId: UUID,
        itemId: UUID,
        nuevoProductId: UUID,
        gramosReales: Int?,
        catalogService: cl.frutapp.backend.modules.catalog.CatalogService,
        context: EventContext
    ) {
        val sustituto = catalogService.product(nuevoProductId.toString())
            ?: throw NotFoundException("Producto sustituto no encontrado.")
        if (!sustituto.disponible) throw ValidationException("El producto sustituto no está disponible.")
        // Ownership atomico: requiere pedido EN_PICKING y assignedPickerId = pickerId.
        val ownerEnPicking = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq STATUS_EN_PICKING)
            }.any()
        }
        if (!ownerEnPicking) throw ValidationException("Este pedido no está en picking o no es tuyo.")
        val updated = orderRepository.sustituirItem(orderId, itemId, sustituto, gramosReales)
        if (updated == 0) throw NotFoundException("Item no encontrado en este pedido.")
        events.logSafely(eventType = "staff.item_sustituido", userId = pickerId, entityType = "order", entityId = orderId, context = context)
    }

    /** Picker reduce la cantidad entregada (mismo producto, menos unidades). */
    suspend fun reducirItem(
        pickerId: UUID,
        orderId: UUID,
        itemId: UUID,
        nuevaCantidad: Int,
        context: EventContext
    ) {
        if (nuevaCantidad <= 0) throw ValidationException("La cantidad debe ser mayor a 0.")
        val ownerEnPicking = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq STATUS_EN_PICKING)
            }.any()
        }
        if (!ownerEnPicking) throw ValidationException("Este pedido no está en picking o no es tuyo.")
        val updated = orderRepository.reducirItem(orderId, itemId, nuevaCantidad)
        if (updated == 0) throw NotFoundException("Item no encontrado en este pedido.")
        events.logSafely(eventType = "staff.item_reducido", userId = pickerId, entityType = "order", entityId = orderId, context = context)
    }

    /** Picker reporta que no tuvo el item — marca SIN_STOCK, monto 0. El total
     *  final del pedido cae en consecuencia. */
    suspend fun reportarFaltante(
        pickerId: UUID,
        orderId: UUID,
        itemId: UUID,
        context: EventContext
    ) {
        val ownerEnPicking = dbQuery {
            OrdersTable.selectAll().where {
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq STATUS_EN_PICKING)
            }.any()
        }
        if (!ownerEnPicking) throw ValidationException("Este pedido no está en picking o no es tuyo.")
        val updated = orderRepository.marcarItemSinStock(orderId, itemId)
        if (updated == 0) throw NotFoundException("Item no encontrado en este pedido.")
        events.logSafely(eventType = "staff.item_faltante", userId = pickerId, entityType = "order", entityId = orderId, context = context)
    }

    /**
     * El picker termina el picking. Recalcula el monto total con los `monto_final` que
     * vino registrando (para items por kg pesados; para unidad usa monto_estimado).
     * Si algun item por kg supero la tolerancia configurada → ESPERANDO_AJUSTE_CLIENTE
     * (espera aprobacion). Si todos dentro de tolerancia → STOCK_CONFIRMADO directo.
     */
    suspend fun complete(pickerId: UUID, orderId: UUID, context: EventContext) {
        val items = orderRepository.listItemsPesoInfo(orderId)
        // NOTA: Antes acá validabamos "todos los items por kg deben estar pesados",
        // pero ese check rompia el flow real del picker que SUSTITUYE o REPORTA
        // FALTANTE un item — esos estados son resoluciones validas sin peso real.
        // El UI del picker exige que NO haya PENDIENTES locales antes de habilitar
        // este boton (PickerPicklistScreen valida pendientes en el cliente), asi
        // que confiar en eso es suficiente para el flow demo.
        //
        // GAP PENDIENTE: el cliente NO propaga al backend cuando sustituye/marca
        // faltante un item. Hoy esos items quedan en PENDIENTE con monto_final
        // null, y calcularTotalFinal cae al monto_estimado (el cliente paga
        // como si recibiera el item original). Para piloto real hay que agregar
        // POST /v1/staff/orders/{id}/items/{itemId}/sustituir y
        // POST /v1/staff/orders/{id}/items/{itemId}/faltante.
        val tolerancia = BusinessConfig.PESO_TOLERANCIA_PORC
        val haySobreTolerancia = items.any { item ->
            // Solo aplica a items por kg con peso pedido + peso real medido.
            if (item.unidad != "kg" || item.gramos == null || item.pesoReal == null) return@any false
            // pesoEsperadoTotal = gramos POR UNIDAD * cantidad (todas las bolsas juntas),
            // pesoReal es el peso TOTAL pesado en bascula. Antes comparaba contra gramos
            // sin multiplicar por cantidad y disparaba ESPERANDO_AJUSTE para pedidos
            // con cantidad>1 aunque el desvio real era pequeno.
            val pesoEsperadoTotal = item.gramos * item.cantidad
            // Defensa contra division por cero: si por algun motivo pesoEsperadoTotal
            // quedo en 0 (no deberia, OrderService.create valida gramos>0), tratamos
            // como dentro de tolerancia para no disparar Infinity en el delta.
            if (pesoEsperadoTotal <= 0) return@any false
            val delta = kotlin.math.abs(item.pesoReal - pesoEsperadoTotal).toDouble() / pesoEsperadoTotal
            delta > tolerancia
        }
        val proximoStatus = if (haySobreTolerancia) STATUS_ESPERANDO_AJUSTE else STATUS_STOCK_CONFIRMADO

        val now = Clock.System.now()
        val ok = dbQuery {
            val updated = OrdersTable.update({
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedPickerId eq pickerId) and
                (OrdersTable.status eq STATUS_EN_PICKING)
            }) {
                it[status] = proximoStatus
                it[updatedAt] = now
            }
            if (updated > 0) {
                recordHistory(
                    orderId,
                    fromStatus = STATUS_EN_PICKING,
                    toStatus = proximoStatus,
                    actorUserId = pickerId,
                    nota = if (haySobreTolerancia) "picker_complete_con_ajuste" else "picker_complete"
                )
                true
            } else false
        }
        if (!ok) throw ValidationException("Este pedido no está en picking o no es tuyo.")

        events.logSafely(
            eventType = if (haySobreTolerancia) "staff.order_pending_ajuste" else "staff.order_completed",
            userId = pickerId, entityType = "order", entityId = orderId,
            context = context
        )

        // El dispatcher mismo se encarga de:
        //  - push al cliente (mensaje segun el estado destino),
        //  - push a repartidores SI el destino es STOCK_CONFIRMADO (lo dispara desde
        //    onOrderTransition cuando ve esa transicion, independiente del origen).
        // Asi tanto el complete() del picker como el aprobar/rechazar del cliente
        // notifican a la cola de despacho sin duplicar codigo aca.
        notifications?.onOrderTransition(
            orderId,
            OrderStatus.EN_PICKING,
            if (haySobreTolerancia) OrderStatus.ESPERANDO_AJUSTE_CLIENTE else OrderStatus.STOCK_CONFIRMADO
        )
    }

    /** Lookup minimo numero + pickup_location del pedido para hooks de push. */
    private suspend fun pickerHomeLocationLookup(orderId: UUID): Pair<String, UUID?>? = dbQuery {
        OrdersTable
            .selectAll().where { OrdersTable.id eq orderId }
            .singleOrNull()
            ?.let { it[OrdersTable.numero] to it[OrdersTable.pickupLocationId] }
    }

    // ============================================================
    // FLUJO REPARTIDOR — Nivel 3
    // ============================================================

    /** Cola de despacho: pedidos STOCK_CONFIRMADO listos para retiro.
     *  Free-for-all por location: el repartidor ve pedidos de SU home_location
     *  (mismo modelo que el picker). Auto-rescate de pedidos atascados en
     *  EN_DESPACHO si el repartidor se desconecto y no completo en 60 min
     *  (mas tiempo que el picker porque entregar lleva mas). */
    suspend fun colaDispatch(repartidorId: UUID): List<StaffDispatchSummaryDto> {
        val rows = dbQuery {
            val location = pickerHomeLocation(repartidorId)
            val rescateThreshold = Clock.System.now().minus(STUCK_DISPATCH_THRESHOLD)
            OrdersTable
                .selectAll()
                .where {
                    (OrdersTable.pickupLocationId eq location) and (
                        (
                            (OrdersTable.status eq STATUS_STOCK_CONFIRMADO) and
                            OrdersTable.assignedRepartidorId.isNull()
                        ) or (
                            (OrdersTable.status eq STATUS_EN_DESPACHO) and
                            OrdersTable.assignedAt.less(rescateThreshold)
                        )
                    )
                }
                .orderBy(OrdersTable.createdAt)
                .limit(50)
                .toList()
        }
        return materializeDispatchSummaries(rows, repartidorId)
    }

    /** Mis despachos en ruta (status EN_DESPACHO, asignados a mi). */
    suspend fun enRutaDispatch(repartidorId: UUID): List<StaffDispatchSummaryDto> {
        val rows = dbQuery {
            OrdersTable
                .selectAll()
                .where {
                    (OrdersTable.assignedRepartidorId eq repartidorId) and
                    (OrdersTable.status eq STATUS_EN_DESPACHO)
                }
                .orderBy(OrdersTable.assignedAt)
                .toList()
        }
        return materializeDispatchSummaries(rows, repartidorId)
    }

    /** Tab "Entregados hoy" del repartidor: despachos que el repartidor logueado
     *  llevo a destino (status=ENTREGADO) en las ultimas 24h. Ordenado del mas
     *  reciente al mas antiguo. Limite 50 — turno tipico no llega ni de cerca. */
    suspend fun entregadosHoyDispatch(repartidorId: UUID): List<StaffDispatchSummaryDto> {
        val ayer = Clock.System.now().minus(24.hours)
        val rows = dbQuery {
            OrdersTable
                .selectAll()
                .where {
                    (OrdersTable.assignedRepartidorId eq repartidorId) and
                    (OrdersTable.status inList ENTREGADOS_REPARTIDOR_STATUSES) and
                    (OrdersTable.updatedAt greater ayer)
                }
                .orderBy(OrdersTable.updatedAt, SortOrder.DESC)
                .limit(50)
                .toList()
        }
        return materializeDispatchSummaries(rows, repartidorId)
    }

    /** Detalle del despacho: cabecera + items + datos de contacto del cliente.
     *  A diferencia del detalle del picker, aqui SI incluimos direccion y
     *  telefono porque el repartidor los necesita para entregar. */
    suspend fun detalleDispatch(repartidorId: UUID, orderId: UUID): StaffDispatchDetailDto {
        data class Cabecera(
            val orderRow: ResultRow,
            val clienteRow: ResultRow?,
            val items: List<StaffOrderItemDto>
        )
        val cabecera = dbQuery {
            val location = pickerHomeLocation(repartidorId)
            val orderRow = OrdersTable
                .selectAll()
                .where { OrdersTable.id eq orderId }
                .singleOrNull()
                ?: throw NotFoundException("Pedido no encontrado.")
            if (orderRow[OrdersTable.pickupLocationId] != location) {
                throw ValidationException("Este pedido no es de tu location.")
            }
            val clienteRow = UsersTable
                .selectAll()
                .where { UsersTable.id eq orderRow[OrdersTable.userId] }
                .singleOrNull()
            val itemsRows = OrderItemsTable
                .selectAll()
                .where { OrderItemsTable.orderId eq orderId }
                .toList()
            val items = itemsRows.mapIndexed { index, row ->
                val unidad = row[OrderItemsTable.unidad]
                val gramos = row[OrderItemsTable.gramos]
                val esKg = unidad == "kg"
                val cantidadDouble = if (esKg && gramos != null) row[OrderItemsTable.cantidad] * gramos / 1000.0
                    else row[OrderItemsTable.cantidad].toDouble()
                StaffOrderItemDto(
                    numero = index + 1,
                    productId = row[OrderItemsTable.productId].toString(),
                    nombre = row[OrderItemsTable.nombre],
                    unidad = unidad,
                    cantidad = cantidadDouble,
                    gramos = gramos,
                    precioUnitario = row[OrderItemsTable.precioUnitario],
                    montoEstimado = row[OrderItemsTable.montoEstimado],
                    pesoVariable = esKg && gramos != null,
                    emoji = emojiForProduct(row[OrderItemsTable.nombre])
                )
            }
            Cabecera(orderRow, clienteRow, items)
        }
        // Resolver avatarUrl FUERA del dbQuery — el resolver hace su propio acceso a
        // BD y a MinIO, no anido transacciones aca.
        val clienteId = cabecera.orderRow[OrdersTable.userId]
        val clienteAvatarUrl = avatarUrlResolver?.invoke(clienteId)
        val clienteNombre = (cabecera.clienteRow?.get(UsersTable.name) ?: "Cliente").substringBefore(' ')
        val telefono = cabecera.clienteRow?.get(UsersTable.phone)
        val orderRow = cabecera.orderRow
        val items = cabecera.items
        return StaffDispatchDetailDto(
            id = orderRow[OrdersTable.id].toString(),
            numero = orderRow[OrdersTable.numero],
            status = orderRow[OrdersTable.status],
            total = orderRow[OrdersTable.totalFinal] ?: orderRow[OrdersTable.totalEstimado],
            createdAt = orderRow[OrdersTable.createdAt].toString(),
            clienteNombre = clienteNombre,
            clienteAvatarUrl = clienteAvatarUrl,
            sector = sectorFromAddress(orderRow[OrdersTable.direccion]),
            direccion = orderRow[OrdersTable.direccion],
            telefono = telefono,
            assignedAt = orderRow[OrdersTable.assignedAt]?.toString(),
            assignedToMe = orderRow[OrdersTable.assignedRepartidorId] == repartidorId,
            items = items
        )
    }

    /** Tomar despacho atomicamente. Status pasa de STOCK_CONFIRMADO a EN_DESPACHO. */
    suspend fun takeDispatch(repartidorId: UUID, orderId: UUID, context: EventContext): StaffTakeResult {
        val now = Clock.System.now()
        val rescateThreshold = now.minus(STUCK_DISPATCH_THRESHOLD)
        val ok = dbQuery {
            val location = pickerHomeLocation(repartidorId)
            val updated = OrdersTable.update({
                (OrdersTable.id eq orderId) and
                (OrdersTable.pickupLocationId eq location) and (
                    (
                        (OrdersTable.status eq STATUS_STOCK_CONFIRMADO) and
                        OrdersTable.assignedRepartidorId.isNull()
                    ) or (
                        (OrdersTable.status eq STATUS_EN_DESPACHO) and
                        OrdersTable.assignedAt.less(rescateThreshold)
                    )
                )
            }) {
                it[assignedRepartidorId] = repartidorId
                it[status] = STATUS_EN_DESPACHO
                it[assignedAt] = now
                it[updatedAt] = now
            }
            if (updated > 0) {
                recordHistory(orderId, fromStatus = STATUS_STOCK_CONFIRMADO, toStatus = STATUS_EN_DESPACHO,
                    actorUserId = repartidorId, nota = "dispatch_take", actorRole = "REPARTIDOR")
                true
            } else false
        }
        if (!ok) return StaffTakeResult(ok = false, motivo = "ya_tomado_o_no_disponible")

        events.logSafely(eventType = "staff.dispatch_taken", userId = repartidorId,
            entityType = "order", entityId = orderId, context = context)
        // Push al cliente: "Tu Repartidor va camino".
        notifications?.onOrderTransition(orderId, OrderStatus.STOCK_CONFIRMADO, OrderStatus.EN_DESPACHO)
        return StaffTakeResult(ok = true, orderId = orderId.toString())
    }

    /** Marcar despacho como ENTREGADO. Status pasa de EN_DESPACHO a ENTREGADO. */
    suspend fun deliveredDispatch(repartidorId: UUID, orderId: UUID, context: EventContext) {
        val now = Clock.System.now()
        val ok = dbQuery {
            val updated = OrdersTable.update({
                (OrdersTable.id eq orderId) and
                (OrdersTable.assignedRepartidorId eq repartidorId) and
                (OrdersTable.status eq STATUS_EN_DESPACHO)
            }) {
                it[status] = STATUS_ENTREGADO
                it[updatedAt] = now
            }
            if (updated > 0) {
                recordHistory(orderId, fromStatus = STATUS_EN_DESPACHO, toStatus = STATUS_ENTREGADO,
                    actorUserId = repartidorId, nota = "dispatch_delivered", actorRole = "REPARTIDOR")
                true
            } else false
        }
        if (!ok) throw ValidationException("Este despacho no está en ruta o no es tuyo.")

        events.logSafely(eventType = "staff.dispatch_delivered", userId = repartidorId,
            entityType = "order", entityId = orderId, context = context)
        // Push al cliente: "Pedido entregado".
        notifications?.onOrderTransition(orderId, OrderStatus.EN_DESPACHO, OrderStatus.ENTREGADO)
    }

    private suspend fun materializeDispatchSummaries(rows: List<ResultRow>, currentRepartidorId: UUID): List<StaffDispatchSummaryDto> {
        if (rows.isEmpty()) return emptyList()
        val userIds = rows.map { it[OrdersTable.userId] }.toSet()
        val userInfo: Map<UUID, Pair<String, String?>> = dbQuery {
            UsersTable
                .selectAll()
                .where { UsersTable.id inList userIds }
                .associate { it[UsersTable.id] to (it[UsersTable.name] to it[UsersTable.phone]) }
        }
        // Resolver avatarUrl una vez por user en paralelo, con cache para no repetir en
        // listas donde el mismo cliente tiene varios pedidos. Si el resolver es null
        // (MinIO no configurado), todos quedan null.
        val avatarUrls: Map<UUID, String?> = userIds.associateWith { uid ->
            avatarUrlResolver?.invoke(uid)
        }

        val orderIds = rows.map { it[OrdersTable.id] }
        val itemsCountPorOrder: Map<UUID, Int> = dbQuery {
            OrderItemsTable
                .selectAll()
                .where { OrderItemsTable.orderId inList orderIds }
                .groupBy { it[OrderItemsTable.orderId] }
                .mapValues { it.value.size }
        }

        return rows.map { row ->
            val orderId = row[OrdersTable.id]
            val clienteId = row[OrdersTable.userId]
            val (nombreFull, telefono) = userInfo[clienteId] ?: ("Cliente" to null)
            val nombreCorto = nombreFull.substringBefore(' ')
            StaffDispatchSummaryDto(
                id = orderId.toString(),
                numero = row[OrdersTable.numero],
                status = row[OrdersTable.status],
                total = row[OrdersTable.totalFinal] ?: row[OrdersTable.totalEstimado],
                itemsCount = itemsCountPorOrder[orderId] ?: 0,
                createdAt = row[OrdersTable.createdAt].toString(),
                clienteNombre = nombreCorto,
                clienteAvatarUrl = avatarUrls[clienteId],
                sector = sectorFromAddress(row[OrdersTable.direccion]),
                direccion = row[OrdersTable.direccion],
                telefono = telefono,
                assignedAt = row[OrdersTable.assignedAt]?.toString(),
                assignedToMe = row[OrdersTable.assignedRepartidorId] == currentRepartidorId
            )
        }
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
        nota: String?,
        actorRole: String = "PICKER"
    ) {
        OrderStatusHistoryTable.insert {
            it[id] = UUID.randomUUID()
            it[OrderStatusHistoryTable.orderId] = orderId
            it[OrderStatusHistoryTable.fromStatus] = fromStatus
            it[OrderStatusHistoryTable.toStatus] = toStatus
            it[actor] = actorRole
            it[OrderStatusHistoryTable.actorUserId] = actorUserId
            it[OrderStatusHistoryTable.nota] = nota
            it[createdAt] = Clock.System.now()
        }
    }

    /** Emoji placeholder por categoria del producto. Heuristica simple por nombre —
     *  cuando exista la columna `image_key` poblada con fotos reales, esto se va. */
    private fun emojiForProduct(nombre: String): String {
        val lower = nombre.lowercase()
        return when {
            lower.contains("palta") || lower.contains("aguacate") -> "🥑"
            lower.contains("tomate") -> "🍅"
            lower.contains("lechuga") || lower.contains("acelga") || lower.contains("espinaca") -> "🥬"
            lower.contains("limón") || lower.contains("limon") -> "🍋"
            lower.contains("plátano") || lower.contains("platano") || lower.contains("banana") -> "🍌"
            lower.contains("manzana") -> "🍎"
            lower.contains("naranja") -> "🍊"
            lower.contains("uva") -> "🍇"
            lower.contains("frutilla") || lower.contains("fresa") -> "🍓"
            lower.contains("zanahoria") -> "🥕"
            lower.contains("pepino") -> "🥒"
            lower.contains("pimiento") || lower.contains("pimentón") || lower.contains("pimenton") -> "🫑"
            lower.contains("cebolla") -> "🧅"
            lower.contains("ajo") -> "🧄"
            lower.contains("papa") -> "🥔"
            else -> "🥬"
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
        const val STATUS_ESPERANDO_AJUSTE = "ESPERANDO_AJUSTE_CLIENTE"
        const val STATUS_EN_DESPACHO = "EN_DESPACHO"
        const val STATUS_ENTREGADO = "ENTREGADO"
        val COLA_LIBRE_STATUSES = listOf(STATUS_CREADO, STATUS_PAGADO)
        // Estados >= STOCK_CONFIRMADO que cuentan como "completados" para el tab
        // "Listos hoy" del picker. ESPERANDO_AJUSTE no esta acá porque todavia
        // espera resolucion del cliente; FACTURADO no esta hoy en el flujo demo
        // pero queda anotado por si se prende. CANCELADO/DEVOLUCION excluidos
        // para no contaminar el conteo de productividad del turno.
        val COMPLETADOS_PICKER_STATUSES = listOf(
            STATUS_STOCK_CONFIRMADO, STATUS_EN_DESPACHO, STATUS_ENTREGADO
        )
        // Estados terminales del repartidor: el despacho llego al cliente.
        val ENTREGADOS_REPARTIDOR_STATUSES = listOf(STATUS_ENTREGADO)
        val STUCK_THRESHOLD = 30.minutes
        // Mas tiempo que el picker porque una entrega lleva mas (calle, trafico, esperar
        // que el cliente baje). Si el repartidor se desconecta, otro puede tomar despues de 60min.
        val STUCK_DISPATCH_THRESHOLD = 60.minutes
    }
}
