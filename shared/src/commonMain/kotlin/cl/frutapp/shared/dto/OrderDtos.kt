package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Crear pedido: el front solo manda qué quiere; el backend re-precia (fuente de verdad). */
@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val direccion: String? = null
)

@Serializable
data class OrderItemRequest(
    val productId: String,
    val cantidad: Int,
    val gramos: Int? = null
)

/** Detalle completo del pedido (lo que la app renderiza, sin calcular nada). */
@Serializable
data class OrderDto(
    val id: String,
    val numero: String,
    val status: String,
    val paymentStatus: String,
    val direccion: String,
    val entrega: String,
    val subtotalEstimado: Int,
    val envio: Int,
    val totalEstimado: Int,
    val totalFinal: Int? = null,
    val frutcoinsGanadas: Int,
    val createdAt: String,
    val items: List<OrderItemDto>
)

@Serializable
data class OrderItemDto(
    val nombre: String,
    val unidad: String,
    val imageKey: String,
    val precioUnitario: Int,
    val gramos: Int? = null,
    val cantidad: Int,
    val montoEstimado: Int,
    val montoFinal: Int? = null,
    val itemStatus: String
)

/** Resumen para la lista "Mis pedidos". */
@Serializable
data class OrderSummaryDto(
    val id: String,
    val numero: String,
    val status: String,
    val total: Int,
    val fecha: String,
    val itemsCount: Int
)

/** Saldo de FrutCoins (derivado del ledger) + movimientos. */
@Serializable
data class FrutCoinsBalanceDto(
    val balance: Int,
    val movimientos: List<FrutCoinsEntryDto>
)

@Serializable
data class FrutCoinsEntryDto(
    val delta: Int,
    val motivo: String,
    val balanceAfter: Int,
    val fecha: String
)

/** Avanzar el estado del pedido (back office). */
@Serializable
data class TransitionRequest(
    val toStatus: String,
    val nota: String? = null
)
