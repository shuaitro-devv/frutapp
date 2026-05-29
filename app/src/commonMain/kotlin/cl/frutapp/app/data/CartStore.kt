package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf

/**
 * Una línea del carrito. `gramos` se usa solo para productos que se venden por kg
 * (250/500/1000); para unidad/atado va null y la cantidad son piezas.
 */
data class CartItem(
    val producto: Producto,
    val cantidad: Int,
    val gramos: Int? = null
) {
    /** Precio total de la línea en CLP. */
    val precioTotal: Int
        get() = if (gramos != null) (producto.precioClp * gramos / 1000.0).toInt() * cantidad
        else producto.precioClp * cantidad

    /** Texto de cantidad para mostrar (ej. "500 g × 2" o "3 unidad"). */
    val detalle: String
        get() = if (gramos != null) {
            val peso = if (gramos >= 1000) "${gramos / 1000} kg" else "$gramos g"
            "$peso × $cantidad"
        } else "$cantidad ${producto.unidad}"
}

/**
 * Carrito de compra en memoria (estado del cliente, igual que en producción el carrito
 * vive en el dispositivo hasta confirmar la compra). Observable por Compose vía
 * [mutableStateListOf]: cualquier pantalla que lea [items] recompone al cambiar.
 */
object CartStore {
    const val ENVIO_GRATIS_DESDE = 15000
    const val COSTO_ENVIO = 2990

    val items = mutableStateListOf<CartItem>()

    val subtotal: Int get() = items.sumOf { it.precioTotal }
    val cantidadTotal: Int get() = items.sumOf { it.cantidad }
    val isEmpty: Boolean get() = items.isEmpty()

    /** Costo de envío: gratis si el carrito está vacío o supera el umbral. */
    val envio: Int get() = if (subtotal == 0 || subtotal >= ENVIO_GRATIS_DESDE) 0 else COSTO_ENVIO
    val total: Int get() = subtotal + envio

    /** Agrega una línea; si ya existe el mismo producto+gramaje, suma la cantidad. */
    fun add(producto: Producto, cantidad: Int = 1, gramos: Int? = null) {
        val idx = items.indexOfFirst { it.producto.id == producto.id && it.gramos == gramos }
        if (idx >= 0) {
            items[idx] = items[idx].copy(cantidad = items[idx].cantidad + cantidad)
        } else {
            items.add(CartItem(producto, cantidad, gramos))
        }
    }

    /** Ubica la línea por identidad (producto + gramaje), no por índice: evita operar
     *  sobre el item equivocado cuando la lista cambia/reordena. */
    private fun indexOf(item: CartItem) =
        items.indexOfFirst { it.producto.id == item.producto.id && it.gramos == item.gramos }

    fun setCantidad(item: CartItem, cantidad: Int) {
        val i = indexOf(item)
        if (i < 0) return
        if (cantidad <= 0) items.removeAt(i) else items[i] = items[i].copy(cantidad = cantidad)
    }

    fun remove(item: CartItem) {
        val i = indexOf(item)
        if (i >= 0) items.removeAt(i)
    }

    fun clear() = items.clear()
}
