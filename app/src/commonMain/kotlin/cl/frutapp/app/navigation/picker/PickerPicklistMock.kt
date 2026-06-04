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
    val emoji: String, // placeholder de foto del producto
    val estado: EstadoItem = EstadoItem.PENDIENTE
)

data class PicklistData(
    val pedidoId: String,
    val sector: String,
    val destino: String,
    val tiempoEstimadoMin: Int,
    val items: List<ItemPicklist>
) {
    val totalItems: Int get() = items.size
    val completados: Int get() = items.count { it.estado != EstadoItem.PENDIENTE }
    val progreso: Float get() = if (totalItems == 0) 0f else completados.toFloat() / totalItems
}

/** Picklist fixture estable para mostrar el mockup en vivo. */
internal fun picklistMock(pedidoId: String): PicklistData = PicklistData(
    pedidoId = pedidoId,
    sector = "Sector Norte",
    destino = "Restaurante Verde",
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
