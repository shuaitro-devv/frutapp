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

        fun parse(value: String): OrderStatus? = entries.firstOrNull { it.name == value }

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
