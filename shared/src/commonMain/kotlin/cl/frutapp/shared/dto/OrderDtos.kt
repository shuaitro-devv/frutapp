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
    val context: ClientContextDto? = null,
    /**
     * Snapshot del config con el que la app calculó el total mostrado al usuario.
     * El backend compara contra su cache: si difiere en una key relevante (envío,
     * umbral de envío gratis), rechaza con 409 + [PricingChangedDto] para que la
     * app re-pinte el total y pida confirmación. Nunca cobramos algo distinto a
     * lo que el cliente vio. Opcional para retrocompatibilidad (clientes viejos
     * pasan sin la red de seguridad, pero el resto del flujo funciona).
     */
    val configSnapshot: Map<String, String>? = null
)

/** Respuesta 409 cuando el snapshot del cliente difiere del cache del backend.
 *  La app muestra `mensaje` en un diálogo, pinta el nuevo total y pide confirmación
 *  para re-enviar con un snapshot fresco. */
@Serializable
data class PricingChangedDto(
    val mensaje: String,
    val nuevoCostoEnvio: Int,
    val nuevoEnvioGratisDesde: Int
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

/** Resumen de pedido que ve el picker/repartidor en su cola. Incluye solo nombre + sector
 *  del cliente (no direccion ni telefono), siguiendo el principio de minimizacion
 *  de la Ley 21.719. La direccion la ve solo el repartidor en el detalle de despacho. */
@Serializable
data class StaffOrderSummaryDto(
    val id: String,
    val numero: String,
    val status: String,
    val total: Int,
    val itemsCount: Int,
    val createdAt: String,
    val clienteNombre: String,     // solo nombre (no apellido si solo hay uno)
    val sector: String,            // ej. "Las Condes", "Providencia"
    val assignedAt: String? = null, // cuando lo tome el picker (null = en cola libre)
    val assignedToMe: Boolean = false // true si soy yo quien lo tomo
)

/** Detalle de un pedido cuando el picker entra al picklist: cabecera + items reales.
 *  Privacidad: solo nombre+sector del cliente, NO direccion ni telefono. */
@Serializable
data class StaffOrderDetailDto(
    val id: String,
    val numero: String,
    val status: String,
    val total: Int,
    val createdAt: String,
    val clienteNombre: String,
    val sector: String,
    val assignedAt: String? = null,
    val assignedToMe: Boolean = false,
    val items: List<StaffOrderItemDto>
)

/** Item individual del pedido en la vista del picker. */
@Serializable
data class StaffOrderItemDto(
    val numero: Int,                 // orden de aparicion en el picklist (1, 2, 3...)
    val productId: String,
    val nombre: String,              // "Palta Hass"
    val unidad: String,              // "kg" / "unidades"
    val cantidad: Double,            // cuanto pidio el cliente (kg o unidades)
    val gramos: Int? = null,         // peso requerido si es kg (para variables)
    val precioUnitario: Int,
    val montoEstimado: Int,
    val pesoVariable: Boolean,       // true si requiere balanza al armar
    val emoji: String                // placeholder visual mientras no haya fotos
)

/** Resumen de despacho que ve el repartidor en su cola. A DIFERENCIA del picker, aca el
 *  repartidor SI necesita ver la direccion completa y el telefono porque tiene que ir
 *  fisicamente a entregar. Sigue siendo principio de minimizacion: nombre + datos de
 *  contacto, NO email ni otra info sensible. */
@Serializable
data class StaffDispatchSummaryDto(
    val id: String,
    val numero: String,
    val status: String,
    val total: Int,
    val itemsCount: Int,
    val createdAt: String,
    val clienteNombre: String,
    val clienteAvatarUrl: String? = null, // URL presignada con TTL — opcional, fallback a inicial
    val sector: String,
    val direccion: String,        // direccion completa (acá si la necesita el repartidor)
    val telefono: String? = null, // contacto para coordinar entrega
    val assignedAt: String? = null,
    val assignedToMe: Boolean = false
)

/** Detalle de un despacho (cuando el repartidor tap "Ver items" o entra al detalle). */
@Serializable
data class StaffDispatchDetailDto(
    val id: String,
    val numero: String,
    val status: String,
    val total: Int,
    val createdAt: String,
    val clienteNombre: String,
    val clienteAvatarUrl: String? = null,
    val sector: String,
    val direccion: String,
    val telefono: String? = null,
    val assignedAt: String? = null,
    val assignedToMe: Boolean = false,
    val items: List<StaffOrderItemDto>
)

/** Respuesta de "tomar pedido": OK o conflicto. */
@Serializable
data class StaffTakeResult(
    val ok: Boolean,
    val orderId: String? = null,
    val motivo: String? = null    // "ya_tomado", "no_encontrado", etc.
)
