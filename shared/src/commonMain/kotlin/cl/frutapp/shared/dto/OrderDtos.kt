package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Crear pedido: el front solo manda qué quiere; el backend re-precia (fuente de verdad). */
@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val direccion: String? = null,
    /** DELIVERY | RETIRO. null = el backend usa la modalidad por defecto. */
    val fulfillmentType: String? = null,
    /** Solo para RETIRO: sucursal de retiro. */
    val sucursal: String? = null,
    /**
     * Medios de pago (puede ser más de uno: pago dividido, incl. FrutCoins).
     * null/vacío = el backend usa un solo medio por defecto. El backend reprecia y
     * decide cuánto cubre cada medio (FrutCoins queda capado por config).
     */
    val payments: List<PaymentInput>? = null,
    /** Contexto del cliente (canal/dispositivo) para soporte y analítica. */
    val context: ClientContextDto? = null
)

@Serializable
data class OrderItemRequest(
    val productId: String,
    val cantidad: Int,
    val gramos: Int? = null
)

/** Un medio de pago dentro del pedido. monto null = el backend asigna el resto del total. */
@Serializable
data class PaymentInput(
    val method: String,        // TARJETA, DEBITO, WEBPAY, MERCADO_PAGO, EFECTIVO, FRUTCOINS, TRANSFERENCIA
    val monto: Int? = null     // CLP que el cliente quiere cubrir con este medio
)

/** Contexto del cliente al crear el pedido (metadata operacional/analítica). */
@Serializable
data class ClientContextDto(
    val channel: String? = null,       // APP_ANDROID, WEB, ...
    val appVersion: String? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val locale: String? = null
)

/** Un medio de pago aplicado al pedido (lo que realmente cubrió cada medio). */
@Serializable
data class OrderPaymentDto(
    val method: String,
    val monto: Int
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
    val items: List<OrderItemDto>,
    val fulfillmentType: String = "DELIVERY",
    val sucursal: String? = null,
    val payments: List<OrderPaymentDto> = emptyList(),
    /** Acciones que el llamante puede ejecutar (estado × sus permisos). Vacío para el
     *  cliente; lo usa el back office para mostrar solo los botones válidos. */
    val allowedActions: List<String> = emptyList()
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
