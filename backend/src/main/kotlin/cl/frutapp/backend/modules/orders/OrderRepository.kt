package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.config.BusinessConfig
import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.auth.UsersTable
import cl.frutapp.shared.dto.OrderDto
import cl.frutapp.shared.dto.OrderItemDto
import cl.frutapp.shared.dto.OrderPaymentDto
import cl.frutapp.shared.dto.OrderSummaryDto
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
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

/** Datos de contacto del cliente dueño de un pedido (back office). */
data class ClienteContacto(
    val nombre: String,
    val email: String,
    val telefono: String?
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

        // V12: location default donde se arma el pedido. Por ahora hay una sola
        // ("Lo Valledor Centro"). Cuando exista admin web podra elegirse en el
        // checkout (cliente mas cerca de su comuna). Nullable defensivo: si la
        // location no existe (datos inconsistentes), igual se crea el pedido —
        // simplemente no aparece en la cola del picker hasta que se le asigne.
        val defaultLocationId = PickupLocationTable
            .selectAll()
            .where { PickupLocationTable.code eq DEFAULT_PICKUP_LOCATION_CODE }
            .firstOrNull()
            ?.get(PickupLocationTable.id)

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
            it[pickupLocationId] = defaultLocationId
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

        insertHistory(orderId, null, OrderStatus.CREADO, OrderActor.CLIENTE, o.userId, "Pedido creado")
        insertHistory(orderId, OrderStatus.CREADO, OrderStatus.PAGADO, OrderActor.SISTEMA, null, "Pago pre-autorizado (simulado)")

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

    /** Back office: nombre/email/teléfono del cliente dueño de un pedido. */
    suspend fun findClienteOf(orderId: UUID): ClienteContacto? = dbQuery {
        val orderRow = OrdersTable
            .select(OrdersTable.userId)
            .where { OrdersTable.id eq orderId }
            .singleOrNull() ?: return@dbQuery null
        val uid = orderRow[OrdersTable.userId]
        UsersTable.selectAll().where { UsersTable.id eq uid }.singleOrNull()?.let {
            ClienteContacto(
                nombre = it[UsersTable.name],
                email = it[UsersTable.email],
                telefono = it[UsersTable.phone]
            )
        }
    }

    /** Heurística simple para el sector visible en la lista ("Av. X, Comuna" -> "Comuna"). */
    private fun sectorFromAddress(direccion: String): String =
        direccion.split(",").map { it.trim() }.lastOrNull { it.isNotEmpty() } ?: "Santiago"

    suspend fun currentStatus(id: UUID): OrderStatus? = dbQuery {
        OrdersTable.selectAll().where { OrdersTable.id eq id }.singleOrNull()
            ?.let { OrderStatus.parse(it[OrdersTable.status]) }
    }

    /** Snapshot por item para calcular ajustes de peso variable.
     *  Incluye lo necesario para: identificar al item, decidir si aplica (unidad=kg),
     *  comparar peso pedido vs real, y recomponer el monto final.
     *  No expone al exterior — solo lo usa StaffOrderService / OrderService internos. */
    data class ItemPesoRow(
        val id: UUID,
        val nombre: String,
        val unidad: String,
        val precioUnitario: Int,
        val gramos: Int?,
        val cantidad: Int,
        val montoEstimado: Int,
        val pesoReal: Int?,
        val montoFinal: Int?,
        val itemStatus: String
    )

    suspend fun listItemsPesoInfo(orderId: UUID): List<ItemPesoRow> = dbQuery {
        OrderItemsTable.selectAll().where { OrderItemsTable.orderId eq orderId }.map {
            ItemPesoRow(
                id = it[OrderItemsTable.id],
                nombre = it[OrderItemsTable.nombre],
                unidad = it[OrderItemsTable.unidad],
                precioUnitario = it[OrderItemsTable.precioUnitario],
                gramos = it[OrderItemsTable.gramos],
                cantidad = it[OrderItemsTable.cantidad],
                montoEstimado = it[OrderItemsTable.montoEstimado],
                pesoReal = it[OrderItemsTable.pesoReal],
                montoFinal = it[OrderItemsTable.montoFinal],
                itemStatus = it[OrderItemsTable.itemStatus]
            )
        }
    }

    /** El picker registra el peso real medido en bascula. Marca el item como CONFIRMADO
     *  y graba `monto_final` ya calculado por el service. Atomico: requiere que el
     *  pedido este EN_PICKING con assignedPickerId = pickerId. Devuelve filas afectadas:
     *  0 = el pedido cambio de estado o ya no es del picker (otro picker rescato por
     *  timeout, o el picker mismo completo). El service traduce 0 a 409/422. */
    suspend fun setItemPeso(orderId: UUID, pickerId: UUID, itemId: UUID, gramosReales: Int, montoFinal: Int): Int = dbQuery {
        // Subquery: ¿este order_id existe + asignado a este picker + en EN_PICKING?
        // Solo si TODO eso es true, hacemos el UPDATE. Esto cierra la race entre el
        // check de ownership en una dbQuery separada y el UPDATE en otra: si entre
        // medio el pedido cambia de estado, el UPDATE deja 0 filas afectadas y el
        // service tira ValidationException sin haber escrito basura.
        val ownerEnPicking = OrdersTable.selectAll().where {
            (OrdersTable.id eq orderId) and
            (OrdersTable.assignedPickerId eq pickerId) and
            (OrdersTable.status eq "EN_PICKING")
        }.any()
        if (!ownerEnPicking) return@dbQuery 0
        OrderItemsTable.update({
            (OrderItemsTable.id eq itemId) and (OrderItemsTable.orderId eq orderId)
        }) {
            it[pesoReal] = gramosReales
            it[OrderItemsTable.montoFinal] = montoFinal
            it[itemStatus] = ITEM_STATUS_CONFIRMADO
        }
    }

    /** Sustituye un item por un producto similar. Preserva nombre/imageKey/
     *  precio_unitario originales; agrega columnas sustituto_* con el dato del
     *  reemplazo y recalcula monto_final con el precio del sustituto. Si el
     *  sustituto es por kg, requiere [gramosReales] para calcular monto; si
     *  es por unidad, se cobra precio_unitario * cantidad_original.
     *  Marca item_status=SUSTITUIDO. */
    suspend fun sustituirItem(
        orderId: UUID,
        itemId: UUID,
        sustituto: cl.frutapp.shared.dto.ProductDto,
        gramosReales: Int?
    ): Int = dbQuery {
        // Buscar el item original para conocer su cantidad (multiplicador).
        val itemRow = OrderItemsTable
            .selectAll().where { (OrderItemsTable.id eq itemId) and (OrderItemsTable.orderId eq orderId) }
            .singleOrNull() ?: return@dbQuery 0
        val cantidad = itemRow[OrderItemsTable.cantidad]
        val esKg = sustituto.unit == "kg"
        // monto_final = precio_unitario * (peso_real_g / 1000) si es kg, o
        // precio_unitario * cantidad si es por unidad. Para kg sin gramosReales
        // usamos los gramos pedidos originales (el sustituto debe pesarse despues).
        val gramos = if (esKg) (gramosReales ?: itemRow[OrderItemsTable.gramos] ?: 1000) else null
        val nuevoMontoFinal = if (esKg && gramos != null) {
            (sustituto.priceClp.toLong() * gramos / 1000L).toInt()
        } else {
            sustituto.priceClp * cantidad
        }
        OrderItemsTable.update({
            (OrderItemsTable.id eq itemId) and (OrderItemsTable.orderId eq orderId)
        }) {
            it[itemStatus] = ITEM_STATUS_SUSTITUIDO
            it[montoFinal] = nuevoMontoFinal
            it[sustitutoNombre] = sustituto.name
            it[sustitutoImageKey] = sustituto.imageKey
            it[sustitutoProductId] = UUID.fromString(sustituto.id)
            if (esKg && gramos != null) it[pesoReal] = gramos
        }
    }

    /** Reduce la cantidad entregada (mismo producto, menos unidades). Recalcula
     *  monto_final con la nueva cantidad. Para items por kg ajusta peso_real
     *  proporcionalmente al ratio nuevo/original (asume que el picker dividio
     *  el gramaje pedido por la cantidad nueva). */
    suspend fun reducirItem(orderId: UUID, itemId: UUID, nuevaCantidad: Int): Int = dbQuery {
        val itemRow = OrderItemsTable
            .selectAll().where { (OrderItemsTable.id eq itemId) and (OrderItemsTable.orderId eq orderId) }
            .singleOrNull() ?: return@dbQuery 0
        val precioUnitario = itemRow[OrderItemsTable.precioUnitario]
        val gramos = itemRow[OrderItemsTable.gramos]
        val nuevoMonto = if (gramos != null) {
            (precioUnitario.toLong() * gramos / 1000L * nuevaCantidad).toInt()
        } else {
            precioUnitario * nuevaCantidad
        }
        OrderItemsTable.update({
            (OrderItemsTable.id eq itemId) and (OrderItemsTable.orderId eq orderId)
        }) {
            it[cantidad] = nuevaCantidad
            it[montoFinal] = nuevoMonto
            it[itemStatus] = ITEM_STATUS_CONFIRMADO  // sigue "confirmado" pero con cantidad ajustada
        }
    }

    /** Marca un item como SIN_STOCK (cliente rechazo el ajuste de este item). */
    suspend fun marcarItemSinStock(orderId: UUID, itemId: UUID): Int = dbQuery {
        OrderItemsTable.update({
            (OrderItemsTable.id eq itemId) and (OrderItemsTable.orderId eq orderId)
        }) {
            it[itemStatus] = ITEM_STATUS_SIN_STOCK
            // monto_final = 0: el item no se cobra. peso_real puede quedar como esta.
            it[montoFinal] = 0
        }
    }

    /** Setea total_final sin tocar el estado. Usado al confirmar/rechazar ajuste para
     *  que la pantalla de confirmacion del cliente muestre el monto correcto. */
    suspend fun setTotalFinal(orderId: UUID, totalFinal: Int) = dbQuery {
        OrdersTable.update({ OrdersTable.id eq orderId }) {
            it[OrdersTable.totalFinal] = totalFinal
            it[updatedAt] = Clock.System.now()
        }
        Unit
    }

    /** Calcula el total final real basandose en los monto_final que dejo el picker
     *  (fallback a monto_estimado si el item no fue pesado: items por unidad/atado, o
     *  items por kg SUSTITUIDOS/FALTANTES por el picker). Le agrega el envio del
     *  pedido. Items en SIN_STOCK suman 0 (no se cobran).
     *
     *  GAP CONOCIDO: el cliente del picker NO propaga al backend cuando sustituye
     *  o marca faltante un item — esos quedan en PENDIENTE con peso_real null
     *  y caen al fallback monto_estimado. El cliente termina pagando como si
     *  recibiera el item original. Para piloto real agregar endpoints
     *  POST /v1/staff/orders/{id}/items/{itemId}/sustituir y .../faltante. */
    suspend fun calcularTotalFinal(orderId: UUID): Int = dbQuery {
        val items = OrderItemsTable.selectAll()
            .where { OrderItemsTable.orderId eq orderId }
            .toList()
        val subtotalReal = items.sumOf { row ->
            val status = row[OrderItemsTable.itemStatus]
            if (status == ITEM_STATUS_SIN_STOCK) 0
            else row[OrderItemsTable.montoFinal] ?: row[OrderItemsTable.montoEstimado]
        }
        val envio = OrdersTable.selectAll()
            .where { OrdersTable.id eq orderId }
            .singleOrNull()
            ?.get(OrdersTable.envio) ?: 0
        subtotalReal + envio
    }

    /** Reintegra al ledger FrutCoins los coins canjeados en este pedido cuando se
     *  cancela. Inserta un asiento positivo (delta = +frutcoinsCanjeadas) con
     *  motivo='REINTEGRO_CANCELACION'. Idempotente: no chequea duplicados (el caller
     *  solo debe llamarlo UNA vez por cancelacion, y la transicion ya esta guarded
     *  por el status del CRITICAL fix). Si no habia coins canjeados, no-op. */
    suspend fun reintegrarFrutCoinsCanjeados(orderId: UUID) = dbQuery {
        val row = OrdersTable
            .selectAll().where { OrdersTable.id eq orderId }
            .singleOrNull() ?: return@dbQuery
        val canjeados = row[OrdersTable.frutcoinsCanjeadas]
        if (canjeados <= 0) return@dbQuery
        val userId = row[OrdersTable.userId]
        // Saldo actual del ledger para mantener balance_after coherente con el resto
        // de filas (no nos confiamos del cache).
        val saldoActual = FrutCoinsLedgerTable
            .selectAll().where { FrutCoinsLedgerTable.userId eq userId }
            .orderBy(FrutCoinsLedgerTable.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .firstOrNull()?.get(FrutCoinsLedgerTable.balanceAfter) ?: 0
        val nuevoSaldo = saldoActual + canjeados
        FrutCoinsLedgerTable.insert {
            it[id] = UUID.randomUUID()
            it[FrutCoinsLedgerTable.userId] = userId
            it[FrutCoinsLedgerTable.orderId] = orderId
            it[delta] = canjeados
            it[motivo] = "REINTEGRO_CANCELACION"
            it[balanceAfter] = nuevoSaldo
            it[createdAt] = Clock.System.now()
        }
        Unit
    }

    /** Picker asignado al pedido + numero. Null si el pedido no esta asignado o no existe.
     *  Usado para notificar al picker cuando el cliente aprueba/rechaza el ajuste de peso. */
    suspend fun findAssignedPickerAndNumero(orderId: UUID): Pair<UUID, String>? = dbQuery {
        OrdersTable
            .select(OrdersTable.assignedPickerId, OrdersTable.numero)
            .where { (OrdersTable.id eq orderId) and OrdersTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let { row ->
                val picker = row[OrdersTable.assignedPickerId] ?: return@let null
                picker to row[OrdersTable.numero]
            }
    }

    /** Dueno del pedido: para validar que el cliente que aprueba/rechaza es el mismo
     *  que lo creo. Excluye soft-deleted. */
    suspend fun findOwner(orderId: UUID): UUID? = dbQuery {
        OrdersTable
            .select(OrdersTable.userId)
            .where { (OrdersTable.id eq orderId) and OrdersTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let { it[OrdersTable.userId] }
    }

    /** Datos minimos para componer notificaciones push al CLIENTE: dueno + numero.
     *  Excluye soft-deleted: si el pedido fue eliminado, no queremos notificar. */
    suspend fun findOwnerAndNumero(id: UUID): Pair<UUID, String>? = dbQuery {
        OrdersTable
            .select(OrdersTable.userId, OrdersTable.numero)
            .where { (OrdersTable.id eq id) and OrdersTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let { it[OrdersTable.userId] to it[OrdersTable.numero] }
    }

    /** Numero + pickup_location_id de un pedido. Usado para componer push a
     *  staff (pickers/repartidores) de la location correspondiente. */
    suspend fun findNumeroAndLocation(id: UUID): Pair<String, UUID?>? = dbQuery {
        OrdersTable
            .select(OrdersTable.numero, OrdersTable.pickupLocationId)
            .where { (OrdersTable.id eq id) and OrdersTable.deletedAt.isNull() }
            .singleOrNull()
            ?.let { it[OrdersTable.numero] to it[OrdersTable.pickupLocationId] }
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
        actorUserId: UUID?,
        nota: String?,
        totalFinal: Int?,
        paymentStatus: PaymentStatus?
    ) = dbQuery {
        // CRITICAL: el WHERE INCLUYE el status esperado para cerrar el TOCTOU entre
        // currentStatus() y este UPDATE. Si dos requests simultaneos pasan
        // canTransition() en memoria, solo UNA va a encontrar la fila en el `from`
        // y persistir; la otra recibe 0 rows y lanza ConflictException para que el
        // caller decida (cliente reintenta con el estado actual; staff ve "ya
        // procesado"). Antes este UPDATE filtraba solo por id → ambos pasaban,
        // duplicando historial y notificaciones.
        val updated = OrdersTable.update({
            (OrdersTable.id eq id) and (OrdersTable.status eq from.name)
        }) {
            it[status] = to.name
            it[updatedAt] = Clock.System.now()
            if (totalFinal != null) it[OrdersTable.totalFinal] = totalFinal
            if (paymentStatus != null) it[OrdersTable.paymentStatus] = paymentStatus.name
        }
        if (updated == 0) {
            throw cl.frutapp.backend.error.ConflictException(
                "Este pedido ya cambió de estado. Refrescá para ver el actual."
            )
        }
        insertHistory(id, from, to, actor, actorUserId, nota)
        Unit
    }

    /**
     * Inserta una fila en order_status_history. [actorUserId] es el UUID del usuario
     * que ejecuto la transicion (V11): null para actores SISTEMA o cuando no se
     * propaga (legacy). Cuando se conoce el user (cliente que crea, picker que
     * toma, admin que avanza), SIEMPRE pasarlo para que la auditoria responda
     * "quien hizo que" al nivel de individuo.
     */
    internal fun insertHistory(
        orderId: UUID,
        from: OrderStatus?,
        to: OrderStatus,
        actor: OrderActor,
        actorUserId: UUID?,
        nota: String?
    ) {
        OrderStatusHistoryTable.insert {
            it[id] = UUID.randomUUID()
            it[OrderStatusHistoryTable.orderId] = orderId
            it[fromStatus] = from?.name
            it[toStatus] = to.name
            it[OrderStatusHistoryTable.actor] = actor.name
            it[OrderStatusHistoryTable.actorUserId] = actorUserId
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
        itemStatus = r[OrderItemsTable.itemStatus],
        id = r[OrderItemsTable.id].toString(),
        pesoReal = r[OrderItemsTable.pesoReal],
        sustitutoNombre = r[OrderItemsTable.sustitutoNombre],
        sustitutoImageKey = r[OrderItemsTable.sustitutoImageKey]
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

    companion object {
        // Codigo de la pickup_location por defecto (seedeada en V12). Cuando exista
        // admin web podra elegirse en checkout segun la comuna del cliente.
        const val DEFAULT_PICKUP_LOCATION_CODE = "lo-valledor-centro"

        // Estados del item (definidos en V4__orders.sql).
        const val ITEM_STATUS_PENDIENTE = "PENDIENTE"
        const val ITEM_STATUS_CONFIRMADO = "CONFIRMADO"
        const val ITEM_STATUS_SUSTITUIDO = "SUSTITUIDO"
        const val ITEM_STATUS_SIN_STOCK = "SIN_STOCK"
    }
}
