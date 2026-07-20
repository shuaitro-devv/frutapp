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

/** Respuesta 409 cuando algun producto del carrito se agoto entre medio. La app
 *  muestra `mensaje` en un dialogo + lista cuales y los descarta del carrito
 *  para que el cliente vuelva a confirmar con lo que quede disponible. */
@Serializable
data class ProductosAgotadosDto(
    val mensaje: String,
    val agotados: List<String>
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
    val allowedActions: List<String> = emptyList(),
    /** Mensajes de chat NO leidos en este pedido para el rol del que consulta
     *  (cliente cuando viene de /v1/orders). 0 si no hay o si la API es vieja. */
    val chatUnread: Int = 0,
    /** Codigo de entrega de 4 digitos. Solo poblado para el cliente del pedido
     *  cuando status=EN_DESPACHO. El cliente se lo dice al repartidor cara a
     *  cara y el backend lo valida al confirmar entrega. NULL para todos los
     *  demas casos (otros status, endpoints staff, pedidos pre-V36). */
    val deliveryCode: String? = null,
    /** Timestamp ISO cuando el repartidor pauso el despacho, si esta pausado.
     *  NULL si no esta pausado (comportamiento default). El cliente ve un
     *  banner "Pausado momentaneamente" en su tracking mientras este campo
     *  no sea null. */
    val dispatchPausedAt: String? = null,
    /** Razon libre de la pausa (ver DispatchPauseReason en la app). NULL
     *  cuando el pedido no esta pausado. */
    val dispatchPauseReason: String? = null,
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
    // Default para retrocompat: backends sin V19 y APKs viejos asumen PENDIENTE.
    // Consistente con StaffOrderItemDto que ya tenia default.
    val itemStatus: String = "PENDIENTE",
    /** Nombre del producto sustituto cuando el picker sustituyo (V22). Null si el
     *  item se entrego tal cual lo pidio el cliente. La app cliente muestra
     *  "Pediste X · Sustituido por Y" si este campo es no-null. */
    val sustitutoNombre: String? = null,
    val sustitutoImageKey: String? = null,
    /** Identificador del item dentro del pedido. La app cliente lo necesita para
     *  identificar items con ajuste; la app picker lo necesita para el endpoint
     *  PUT /v1/staff/orders/{id}/items/{itemId}/peso. */
    val id: String? = null,
    /** Peso real medido por el picker (solo para unidad=kg). null = aun no pesado. */
    val pesoReal: Int? = null
)

/** Request del picker para registrar el peso real medido en bascula. */
@Serializable
data class SetItemPesoRequest(
    /** Peso real en gramos (no en kg). Ej. 1200 = 1.2 kg. Debe ser > 0. */
    val gramosReales: Int
)

/** Request del picker para sustituir un item por un producto similar. El backend
 *  recalcula monto_final con el precio del sustituto y marca el item como
 *  SUSTITUIDO. Preserva nombre/imageKey original para que el cliente vea
 *  "pediste X, recibiste Y". */
@Serializable
data class SustituirItemRequest(
    /** UUID del producto sustituto. Debe estar disponible (catálogo). */
    val nuevoProductId: String,
    /** Solo si el sustituto es por kg: peso real medido. Si null y el sustituto
     *  es kg, asume gramos pedidos originales. */
    val gramosReales: Int? = null
)

/** Request del picker para reducir la cantidad entregada (mismo producto, menos
 *  unidades). El backend recalcula monto_final. */
@Serializable
data class ReducirItemRequest(
    /** Cantidad nueva, debe ser > 0 y < cantidad original. */
    val nuevaCantidad: Int
)

/** Resumen del ajuste pendiente: items que excedieron la tolerancia + delta del total.
 *  El cliente ve esto en la pantalla de aprobacion antes de decidir aprobar o rechazar. */
@Serializable
data class AjusteResumenDto(
    val orderId: String,
    val numero: String,
    val totalEstimadoOriginal: Int,
    val totalAjustado: Int,
    /** Items que excedieron la tolerancia (los que el cliente va a aprobar/rechazar). */
    val itemsAjustados: List<ItemAjusteDto>,
    /** Items dentro de tolerancia: ajustados sin pedir aprobacion (informativo). */
    val itemsDentroTolerancia: List<ItemAjusteDto>
)

@Serializable
data class ItemAjusteDto(
    val nombre: String,
    val unidad: String,
    val imageKey: String,
    val gramosPedidos: Int,
    val gramosReales: Int,
    val cantidad: Int,
    val montoEstimado: Int,
    val montoFinal: Int,
    /** Variación porcentual respecto al pedido. Ej. 0.15 = +15%, -0.08 = -8%. */
    val deltaPorc: Double
)

/** Resumen para la lista "Mis pedidos". */
@Serializable
data class OrderSummaryDto(
    val id: String,
    val numero: String,
    val status: String,
    val total: Int,
    val fecha: String,
    val itemsCount: Int,
    /** Mensajes de chat NO leidos del cliente en este pedido. 0 si no hay. */
    val chatUnread: Int = 0,
)

/** Body para POST /v1/staff/orders/dispatch/{id}/delivered. El repartidor
 *  envia el codigo que el cliente le dijo cara a cara; el backend valida
 *  contra el delivery_code guardado al EN_DESPACHO. */
@Serializable
data class ConfirmarEntregaRequest(
    val codigo: String,
)

/** Body para POST /v1/staff/orders/dispatch/{id}/pause. El repartidor pausa
 *  o reanuda el despacho con el mismo endpoint (toggle). Reason obligatorio
 *  al pausar (semaforo, emergencia, otro), ignorado al reanudar. */
@Serializable
data class PausarDespachoRequest(
    val pausar: Boolean,
    val reason: String? = null,
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
    /** Cuando se actualizo por ultima vez (updatedAt del row). En estados
     *  terminales-para-el-picker (STOCK_CONFIRMADO/EN_DESPACHO/ENTREGADO/FACTURADO)
     *  representa el momento del armado completo, asi el UI puede decir "hace X min"
     *  desde que TERMINO en lugar de desde que lo TOMO. Opcional para retrocompat. */
    val updatedAt: String? = null,
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
    val emoji: String,               // fallback visual si no hay drawable bundleado
    /** Slug de la imagen bundleada del producto. La app picker hace lookup local
     *  con [brandProductDrawable] (mismo patron que la app cliente) y cae al
     *  emoji si el slug no tiene drawable mapeado. */
    val imageKey: String? = null,
    /** UUID del item — el picker lo necesita para el endpoint PUT .../items/{id}/peso. */
    val id: String? = null,
    /** Peso real medido por el picker (null hasta confirmar en bascula). */
    val pesoReal: Int? = null,
    /** Estado del item: PENDIENTE / CONFIRMADO / SUSTITUIDO / SIN_STOCK. */
    val itemStatus: String = "PENDIENTE"
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
    /** Mismo proposito que [StaffOrderSummaryDto.updatedAt]: tiempo del armado
     *  completo para mostrar "hace X min" desde que TERMINO, no desde que lo TOMO. */
    val updatedAt: String? = null,
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
    val items: List<StaffOrderItemDto>,
    /** Timestamp cuando el repartidor pauso el despacho (null = no pausado).
     *  La app del repartidor lo usa para renderizar "Pausar" o "Reanudar". */
    val dispatchPausedAt: String? = null,
    val dispatchPauseReason: String? = null,
)

/** Respuesta de "tomar pedido": OK o conflicto. */
@Serializable
data class StaffTakeResult(
    val ok: Boolean,
    val orderId: String? = null,
    val motivo: String? = null    // "ya_tomado", "no_encontrado", etc.
)
