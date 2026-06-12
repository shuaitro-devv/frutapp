package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.config.BusinessConfig
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.PricingChangedException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.catalog.CatalogRepository
import cl.frutapp.shared.dto.AjusteResumenDto
import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.FrutCoinsBalanceDto
import cl.frutapp.shared.dto.ItemAjusteDto
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
    private val onTransitionFired: ((orderId: java.util.UUID, from: OrderStatus, to: OrderStatus) -> Unit)? = null,
    /** Hook opcional: fired tras crear un pedido, para notificar a pickers
     *  de la location correspondiente. Mismas reglas null en tests. */
    private val onOrderCreated: ((orderId: java.util.UUID, locationId: java.util.UUID, numero: String) -> Unit)? = null
) {

    suspend fun create(userId: UUID, req: CreateOrderRequest): OrderDto {
        if (req.items.isEmpty()) throw ValidationException("El carrito está vacío.")
        // Snapshot del config tomado AHORA y reusado para validar Y precificar la
        // orden. Sin esto habia un TOCTOU: si ConfigCache se refrescaba entre la
        // validacion del snapshot y el calculo del envio, validabamos contra una
        // version y cobrabamos otra.
        val costoEnvioActual = BusinessConfig.COSTO_ENVIO
        val envioGratisActual = BusinessConfig.ENVIO_GRATIS_DESDE
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
            subtotal == 0 || subtotal >= envioGratisActual -> 0
            else -> costoEnvioActual
        }

        // Validacion del snapshot AHORA (despues de saber subtotal + modalidad): comparamos
        // el ENVIO EFECTIVO con el config del cliente vs el config actual. Asi NO rechazamos
        // ordenes donde el cliente iba a pagar lo mismo igual (retiro, o subtotal sobre umbral),
        // y SI rechazamos cuando la diferencia realmente cambia lo que se cobra.
        req.configSnapshot?.let { snap ->
            val costoSnap = snap["costo_envio"]?.toIntOrNull() ?: costoEnvioActual
            val gratisSnap = snap["envio_gratis_desde"]?.toIntOrNull() ?: envioGratisActual
            val envioConSnap = when {
                esRetiro -> 0
                subtotal == 0 || subtotal >= gratisSnap -> 0
                else -> costoSnap
            }
            if (envioConSnap != envio) throw PricingChangedException(
                nuevoCostoEnvio = costoEnvioActual,
                nuevoEnvioGratisDesde = envioGratisActual
            )
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
        // Push fire-and-forget a pickers de la location: "pedido nuevo en cola".
        // Lookup minimo del pickup_location_id (NewOrder NO lo expone aca; el
        // repo lo asigno con su default). Si el pedido no tiene location todavia
        // (legacy / migration en curso) el hook se descarta silenciosamente.
        if (onOrderCreated != null) {
            orders.findNumeroAndLocation(id)?.let { (numeroDb, locId) ->
                if (locId != null) onOrderCreated.invoke(id, locId, numeroDb)
            }
        }
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

    /**
     * Resumen del ajuste de peso pendiente que el cliente ve en su pantalla de
     * aprobacion. Solo tiene sentido en estado ESPERANDO_AJUSTE_CLIENTE.
     *  - `itemsAjustados`: pasaron el umbral de tolerancia (los que ENG aprueba/rechaza).
     *  - `itemsDentroTolerancia`: tambien cambiaron, pero no requieren aprobacion
     *    (informativos en la pantalla).
     */
    suspend fun getResumenAjuste(userId: UUID, orderIdStr: String): AjusteResumenDto {
        val orderId = parseUuid(orderIdStr, "Id inválido.")
        val owner = orders.findOwner(orderId) ?: throw NotFoundException("Pedido no encontrado.")
        if (owner != userId) throw NotFoundException("Pedido no encontrado.") // 404 en lugar de 403: no filtramos existencia
        val current = orders.currentStatus(orderId) ?: throw NotFoundException("Pedido no encontrado.")
        if (current != OrderStatus.ESPERANDO_AJUSTE_CLIENTE) {
            throw ValidationException("Este pedido no tiene un ajuste pendiente.")
        }
        val full = orders.findById(orderId) ?: throw NotFoundException("Pedido no encontrado.")
        val items = orders.listItemsPesoInfo(orderId)
        val tolerancia = BusinessConfig.PESO_TOLERANCIA_PORC

        val (sobre, dentro) = items
            .filter { it.unidad == "kg" && it.gramos != null && it.pesoReal != null && it.montoFinal != null }
            .map { item ->
                // Comparamos contra el peso esperado TOTAL (gramos por unidad * cantidad).
                // pesoReal es el peso total pesado en bascula.
                val pesoEsperadoTotal = item.gramos!! * item.cantidad
                val delta = (item.pesoReal!! - pesoEsperadoTotal).toDouble() / pesoEsperadoTotal
                ItemAjusteDto(
                    nombre = item.nombre,
                    unidad = item.unidad,
                    imageKey = "",  // el cliente lo tiene de OrderDto; ItemAjusteDto es solo el delta.
                    gramosPedidos = pesoEsperadoTotal,
                    gramosReales = item.pesoReal,
                    cantidad = item.cantidad,
                    montoEstimado = item.montoEstimado,
                    montoFinal = item.montoFinal!!,
                    deltaPorc = delta
                )
            }
            .partition { kotlin.math.abs(it.deltaPorc) > tolerancia }

        // total ajustado SI EL CLIENTE APRUEBA: suma monto_final real + envio.
        val totalAjustado = orders.calcularTotalFinal(orderId)
        return AjusteResumenDto(
            orderId = orderId.toString(),
            numero = full.numero,
            totalEstimadoOriginal = full.totalEstimado,
            totalAjustado = totalAjustado,
            itemsAjustados = sobre,
            itemsDentroTolerancia = dentro
        )
    }

    /** El cliente acepta el ajuste de peso → ESPERANDO_AJUSTE_CLIENTE → STOCK_CONFIRMADO. */
    suspend fun aprobarAjuste(userId: UUID, orderIdStr: String): OrderDto {
        val orderId = parseUuid(orderIdStr, "Id inválido.")
        val owner = orders.findOwner(orderId) ?: throw NotFoundException("Pedido no encontrado.")
        if (owner != userId) throw NotFoundException("Pedido no encontrado.")
        val from = orders.currentStatus(orderId) ?: throw NotFoundException("Pedido no encontrado.")
        if (from != OrderStatus.ESPERANDO_AJUSTE_CLIENTE) {
            throw ValidationException("Este pedido no tiene un ajuste pendiente.")
        }
        applyStep(orderId, from, OrderStatus.STOCK_CONFIRMADO, OrderActor.CLIENTE, userId, "cliente_aprobo_ajuste")
        return orders.findById(orderId) ?: throw NotFoundException("Pedido no encontrado.")
    }

    /** El cliente rechaza los items con delta > tolerancia: se marcan SIN_STOCK (no se
     *  cobran), el resto sigue, total_final se recalcula sin esos items. Va a
     *  STOCK_CONFIRMADO con el total reducido. */
    suspend fun rechazarAjuste(userId: UUID, orderIdStr: String): OrderDto {
        val orderId = parseUuid(orderIdStr, "Id inválido.")
        val owner = orders.findOwner(orderId) ?: throw NotFoundException("Pedido no encontrado.")
        if (owner != userId) throw NotFoundException("Pedido no encontrado.")
        val from = orders.currentStatus(orderId) ?: throw NotFoundException("Pedido no encontrado.")
        if (from != OrderStatus.ESPERANDO_AJUSTE_CLIENTE) {
            throw ValidationException("Este pedido no tiene un ajuste pendiente.")
        }

        // Rechazo basado en LO QUE EL CLIENTE VIO: marcamos SIN_STOCK los items por kg
        // donde el peso real difiere del pedido (delta != 0). NO usamos la tolerancia
        // actual porque pudo haberse editado entre que el cliente abrio la pantalla
        // y aprobo — el cliente decide sobre el snapshot que tenia en pantalla, no
        // sobre la regla actual del operador.
        val items = orders.listItemsPesoInfo(orderId)
        val itemsARechazar = items.filter { item ->
            item.unidad == "kg" && item.gramos != null && item.pesoReal != null && run {
                val pesoEsperadoTotal = item.gramos * item.cantidad
                item.pesoReal != pesoEsperadoTotal
            }
        }
        itemsARechazar.forEach { orders.marcarItemSinStock(orderId, it.id) }

        // Si el cliente rechazo TODOS los items con peso (el pedido queda en saco
        // vacio: solo envio), cancelamos en vez de pasar a STOCK_CONFIRMADO — no
        // tiene sentido despachar nada. El payment va a REEMBOLSADO via applyStep.
        val totalTrasRechazo = orders.calcularTotalFinal(orderId)
        val subtotalTrasRechazo = totalTrasRechazo - (orders.findById(orderId)?.envio ?: 0)
        val destino = if (subtotalTrasRechazo <= 0) OrderStatus.CANCELADO else OrderStatus.STOCK_CONFIRMADO
        applyStep(orderId, from, destino, OrderActor.CLIENTE, userId, "cliente_rechazo_items_ajuste")
        return orders.findById(orderId) ?: throw NotFoundException("Pedido no encontrado.")
    }

    /** Aplica una transición ya validada, con los efectos de negocio (captura/reembolso). */
    private suspend fun applyStep(id: UUID, from: OrderStatus, to: OrderStatus, actor: OrderActor, actorUserId: UUID?, nota: String?) {
        // Al confirmar stock fijamos el total final REAL (suma de monto_final por item,
        // fallback a monto_estimado) + envio. Antes usabamos solo el estimado, lo que
        // ignoraba ajustes de peso dentro de tolerancia (1kg -> 1.05kg cobraba el
        // estimado, no el real). Y captura el pago.
        val totalFinal = if (to == OrderStatus.STOCK_CONFIRMADO) orders.calcularTotalFinal(id) else null
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
