package cl.frutapp.backend.modules.orders

/**
 * Máquina de estados del pedido. Las transiciones válidas se validan en el servicio
 * (el back office no puede "saltar" pasos). Happy path + excepciones.
 */
enum class OrderStatus {
    CREADO,
    PAGADO,
    EN_PICKING,
    /** El picker confirmo stock pero algun item por kg salio con un delta de peso
     *  superior a la tolerancia configurada (peso_tolerancia_porc). El pedido se
     *  detiene aca hasta que el cliente apruebe el ajuste o rechace los items
     *  afectados — recien ahi pasa a STOCK_CONFIRMADO. Bajo tolerancia, no se
     *  pasa por aqui (UX fluida en el caso comun). */
    ESPERANDO_AJUSTE_CLIENTE,
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
            EN_PICKING to setOf(STOCK_CONFIRMADO, ESPERANDO_AJUSTE_CLIENTE, CANCELADO),
            ESPERANDO_AJUSTE_CLIENTE to setOf(STOCK_CONFIRMADO, CANCELADO),
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
            // ESPERANDO_AJUSTE_CLIENTE lo dispara el sistema (al confirmar stock con delta).
            // No es una accion manual, no tiene permiso para back office.
            ESPERANDO_AJUSTE_CLIENTE -> null
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
         *  null = estado terminal o que no avanza solo. Lo usa el auto-avance de demo.
         *  ESPERANDO_AJUSTE_CLIENTE NO tiene "next happy" automatico: requiere accion
         *  del cliente, asi que el auto-avance del demo lo salta. */
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

/** Estado del pago en relacion al ciclo de captura de la pasarela.
 *  - PREAUTORIZADO: hold del monto, sin debito real (Webpay/MP held).
 *  - CAPTURADO: el cobro fue efectivo (lo movio del cliente al merchant).
 *  - REVERSADO: cancelacion ANTES de captura (void del hold). En contabilidad
 *    no genera asiento de reembolso porque el dinero nunca cambio de cuenta;
 *    el hold se libera. Es el path para cancelar EN_PICKING / ESPERANDO_AJUSTE.
 *  - REEMBOLSADO: cancelacion DESPUES de captura, requiere refund real via
 *    la pasarela. Path para devoluciones post-entrega. */
enum class PaymentStatus { PREAUTORIZADO, CAPTURADO, REVERSADO, REEMBOLSADO }

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
