package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/**
 * DTOs del back office para ver pedidos a nivel global (no scopeados al usuario).
 * Los consume el panel web; la app cliente NO los usa.
 */

/** Resumen de un pedido en la lista del día del back office. */
@Serializable
data class AdminOrderSummaryDto(
    val id: String,
    val numero: String,
    val status: String,
    val paymentStatus: String,
    val total: Int,
    val itemsCount: Int,
    val createdAt: String,
    val clienteNombre: String,
    val sector: String,
    val fulfillmentType: String,
)

/** Página de "pedidos del día" + métricas agregadas (ticket promedio, total). */
@Serializable
data class AdminOrdersPageDto(
    val orders: List<AdminOrderSummaryDto>,
    val count: Int,
    /** Promedio de los totales de la página (0 si no hay pedidos). */
    val ticketPromedio: Int,
    /** Suma de los totales de la página. */
    val totalDia: Int,
    /** Fecha consultada (YYYY-MM-DD, zona America/Santiago). */
    val fecha: String,
)

/** Detalle de un pedido para el back office: el pedido completo + datos del cliente. */
@Serializable
data class AdminOrderDetailDto(
    val order: OrderDto,
    val clienteNombre: String,
    val clienteEmail: String,
    val clienteTelefono: String? = null,
)
