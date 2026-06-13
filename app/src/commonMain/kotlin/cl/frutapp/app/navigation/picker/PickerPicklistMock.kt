package cl.frutapp.app.navigation.picker

/**
 * Mock data del picklist (detalle del pedido en preparacion). Vive aca hasta que existan
 * los endpoints reales del picker en el backend. Cuando se cablee a API se elimina y los
 * items vienen del repositorio.
 */

/**
 * Estado de cada item del picklist. El pedido solo cierra (boton 'Marcar como listo' se
 * habilita) cuando ningun item esta PENDIENTE — cada uno tiene que estar resuelto en
 * alguna forma. Esto fuerza al picker a indicar QUE paso con lo que no logro obtener,
 * lo que da el feedback loop para mejorar surtido/proveedor/SLA en lugar de un balde
 * negro de 'el pedido salio con cosas faltantes pero no sabemos cuales'.
 *
 *  - PENDIENTE: el picker todavia no toco el item.
 *  - COMPLETADO: encontrado tal cual; incluye peso variable confirmado.
 *  - SUSTITUIDO: cambiado por una alternativa similar (modal sustitucion).
 *  - REDUCIDO: cantidad menor a la solicitada (modal sustitucion → reducir).
 *  - FALTANTE: reportado sin reemplazo posible (modal sustitucion → reportar faltante).
 */
enum class EstadoItem { PENDIENTE, COMPLETADO, SUSTITUIDO, REDUCIDO, FALTANTE }

/** Resuelto = ya no requiere accion del picker (boton 'listo' puede desbloquearse). */
fun EstadoItem.resuelto(): Boolean = this != EstadoItem.PENDIENTE

data class ItemPicklist(
    val numero: Int,
    val nombre: String,
    val cantidad: Double,
    val unidad: String, // "kg", "unidades"
    val pasillo: String,
    val estante: String,
    val pesoVariable: Boolean,
    val emoji: String, // fallback si no hay drawable bundleado
    val estado: EstadoItem = EstadoItem.PENDIENTE,
    /** UUID del item en el backend (StaffOrderItemDto.id). Null en modo mock. */
    val backendId: String? = null,
    /** Slug de la imagen del producto. Si tiene drawable mapeado, el picker
     *  lo renderiza con [brandProductDrawable]; si no, cae al [emoji]. */
    val imageKey: String? = null,
    /** UUID del PRODUCTO en el backend (StaffOrderItemDto.productId). Lo usa el
     *  SustitucionModal para pedir alternativas similares al backend. Null en
     *  modo mock. */
    val backendProductId: String? = null
)

data class PicklistData(
    val pedidoId: String,
    val sector: String,
    val destino: String,
    val tiempoEstimadoMin: Int,
    val items: List<ItemPicklist>,
    /** ISO timestamp de cuando el picker tomo el pedido (assignedAt del backend).
     *  Null en modo mock o si todavia no se tomo. Se usa para calcular la duracion
     *  del armado en la pantalla "Pedido listo". */
    val tomadoEnIso: String? = null
) {
    val totalItems: Int get() = items.size
    val completados: Int get() = items.count { it.estado != EstadoItem.PENDIENTE }
    val progreso: Float get() = if (totalItems == 0) 0f else completados.toFloat() / totalItems
}

/**
 * Catálogo de destinos mock — picklistMock elige uno deterministicamente a partir del
 * pedidoId para que distintos pedidos vistos durante la demo no muestren TODOS el mismo
 * 'Sector Norte / Restaurante Verde'. Cuando se conecte al backend, este lookup
 * desaparece y los datos vienen del orden real.
 */
private val DESTINOS_MOCK = listOf(
    "Sector Norte" to "Restaurante Verde",
    "Sector Centro" to "Hotel Sol",
    "Sector Sur" to "Café del Parque",
    "Sector Oeste" to "Tienda Natural",
    "Sector Centro" to "Oficina Central",
    "Sector Norte" to "Bistró La Esquina",
    "Sector Sur" to "Almacén San Pedro"
)

/** Picklist fixture variable por pedidoId (destino/sector cambian) para evitar la
 *  contradicción visual 'todos los pedidos van al mismo restaurante' durante la demo. */
internal fun picklistMock(pedidoId: String): PicklistData {
    val idx = (kotlin.math.abs(pedidoId.hashCode()) % DESTINOS_MOCK.size)
    val (sectorElegido, destinoElegido) = DESTINOS_MOCK[idx]
    return PicklistData(
    pedidoId = pedidoId,
    sector = sectorElegido,
    destino = destinoElegido,
    tiempoEstimadoMin = 18,
    items = listOf(
        ItemPicklist(1, "Palta Hass", 3.0, "kg", "B", "04", true, "🥑", EstadoItem.COMPLETADO),
        ItemPicklist(2, "Tomate", 4.0, "kg", "B", "02", false, "🍅", EstadoItem.PENDIENTE),
        ItemPicklist(3, "Lechuga Romana", 6.0, "unidades", "C", "01", false, "🥬", EstadoItem.PENDIENTE),
        ItemPicklist(4, "Limón Sutil", 2.0, "kg", "B", "06", false, "🍋", EstadoItem.PENDIENTE),
        ItemPicklist(5, "Plátano Cavendish", 5.0, "kg", "A", "05", false, "🍌", EstadoItem.COMPLETADO),
        ItemPicklist(6, "Manzana Roja", 8.0, "unidades", "A", "03", false, "🍎", EstadoItem.PENDIENTE),
        ItemPicklist(7, "Cebolla", 3.0, "kg", "C", "08", false, "🧅", EstadoItem.PENDIENTE),
        ItemPicklist(8, "Zanahoria", 4.0, "kg", "C", "09", false, "🥕", EstadoItem.PENDIENTE),
        ItemPicklist(9, "Pepino", 6.0, "unidades", "B", "10", false, "🥒", EstadoItem.PENDIENTE),
        ItemPicklist(10, "Ají Verde", 1.0, "kg", "D", "01", true, "🌶️", EstadoItem.PENDIENTE),
        ItemPicklist(11, "Espinaca", 2.0, "kg", "C", "04", false, "🥗", EstadoItem.PENDIENTE),
        ItemPicklist(12, "Brócoli", 3.0, "unidades", "C", "05", false, "🥦", EstadoItem.PENDIENTE)
    )
    )
}
