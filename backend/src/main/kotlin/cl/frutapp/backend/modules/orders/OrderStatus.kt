package cl.frutapp.backend.modules.orders

/**
 * Máquina de estados del pedido. Las transiciones válidas se validan en el servicio
 * (el back office no puede "saltar" pasos). Happy path + excepciones.
 */
enum class OrderStatus {
    CREADO,
    PAGADO,
    EN_PICKING,
    STOCK_CONFIRMADO,
    FACTURADO,
    EN_DESPACHO,
    ENTREGADO,
    CANCELADO,
    DEVOLUCION;

    companion object {
        private val transitions: Map<OrderStatus, Set<OrderStatus>> = mapOf(
            CREADO to setOf(PAGADO, CANCELADO),
            PAGADO to setOf(EN_PICKING, CANCELADO),
            EN_PICKING to setOf(STOCK_CONFIRMADO, CANCELADO),
            STOCK_CONFIRMADO to setOf(FACTURADO, CANCELADO),
            FACTURADO to setOf(EN_DESPACHO),
            EN_DESPACHO to setOf(ENTREGADO),
            ENTREGADO to setOf(DEVOLUCION),
            CANCELADO to emptySet(),
            DEVOLUCION to emptySet()
        )

        fun canTransition(from: OrderStatus, to: OrderStatus): Boolean =
            to in (transitions[from] ?: emptySet())

        fun nextStates(from: OrderStatus): Set<OrderStatus> = transitions[from] ?: emptySet()

        fun parse(value: String): OrderStatus? = entries.firstOrNull { it.name == value }

        /** Permiso requerido para mover (manualmente, back office) un pedido HACIA `to`.
         *  null = transición de sistema (no es una acción manual). */
        fun permissionFor(to: OrderStatus): String? = when (to) {
            EN_PICKING -> "order:pick"
            STOCK_CONFIRMADO -> "order:confirm_stock"
            FACTURADO -> "order:invoice"
            EN_DESPACHO -> "order:dispatch"
            ENTREGADO -> "order:deliver"
            CANCELADO -> "order:cancel"
            DEVOLUCION -> "order:cancel"
            else -> null // CREADO/PAGADO los hace el sistema
        }

        /** Acciones que un llamante con `permissions` puede ejecutar sobre un pedido en
         *  estado `from` = próximos estados válidos para los que tiene el permiso. */
        fun allowedActions(from: OrderStatus, permissions: Set<String>): List<String> =
            nextStates(from).filter { to -> permissionFor(to)?.let { it in permissions } == true }.map { it.name }

        /** Siguiente estado del "camino feliz" (sin ramas de cancelación/devolución).
         *  null = estado terminal o que no avanza solo. Lo usa el auto-avance de demo. */
        fun nextHappy(from: OrderStatus): OrderStatus? = when (from) {
            PAGADO -> EN_PICKING
            EN_PICKING -> STOCK_CONFIRMADO
            STOCK_CONFIRMADO -> FACTURADO
            FACTURADO -> EN_DESPACHO
            EN_DESPACHO -> ENTREGADO
            else -> null
        }
    }
}

enum class PaymentStatus { PREAUTORIZADO, CAPTURADO, REEMBOLSADO }

enum class OrderActor { CLIENTE, SISTEMA, OPERADOR, REPARTIDOR }

/** Medios de pago aceptados (uno o varios por pedido, pago dividido). */
enum class PaymentMethod {
    TARJETA, DEBITO, WEBPAY, MERCADO_PAGO, EFECTIVO, FRUTCOINS, TRANSFERENCIA;

    companion object {
        fun parse(value: String): PaymentMethod? = entries.firstOrNull { it.name == value }
    }
}

/** Modalidad de entrega del pedido. */
enum class FulfillmentType {
    DELIVERY, RETIRO;

    companion object {
        fun parse(value: String): FulfillmentType? = entries.firstOrNull { it.name == value }
    }
}
