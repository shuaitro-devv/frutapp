package cl.frutapp.app.data

import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.shared.dto.OrderItemDto

/** Resultado de re-armar el carrito: cuántos ítems se agregaron de cuántos totales. */
data class ReorderResult(val agregados: Int, val total: Int)

/**
 * Re-arma el carrito a partir de los ítems de un pedido. Los ítems son snapshots
 * (sin productId), así que se re-mapean al catálogo actual por imageKey para recuperar
 * el producto real (con su id) y poder volver a pagar. Devuelve null si no se pudo
 * cargar el catálogo.
 */
suspend fun reorderIntoCart(items: List<OrderItemDto>): ReorderResult? {
    val catalogo = runCatching { CatalogApi().products() }.getOrNull()
    if (catalogo.isNullOrEmpty()) return null
    val porImagen = catalogo.associateBy { it.imageKey }
    var agregados = 0
    items.forEach { item ->
        porImagen[item.imageKey]?.let { p ->
            CartStore.add(p.toProducto(), item.cantidad, item.gramos)
            agregados++
        }
    }
    return ReorderResult(agregados, items.size)
}

/** Mensaje de feedback estándar para el resultado de re-pedir (incluye el caso null). */
fun ReorderResult?.toastMessage(): String = when {
    this == null -> "No pudimos cargar el catálogo"
    agregados == 0 -> "No pudimos re-armar este pedido"
    agregados == total -> "Productos agregados al carrito"
    else -> "$agregados de $total productos agregados"
}

/** True si se agregó al menos un producto (corresponde navegar al carrito). */
fun ReorderResult?.huboItems(): Boolean = this != null && agregados > 0

/**
 * Convierte los ítems de un pedido a [CanastaItem]s resolviendo contra el catálogo del
 * backend por imageKey (igual mecanismo que [reorderIntoCart]). Devuelve lista vacía si
 * el catálogo no se pudo cargar.
 */
suspend fun pedidoToCanastaItems(items: List<OrderItemDto>): List<CanastaItem> {
    val catalogo = runCatching { CatalogApi().products() }.getOrNull().orEmpty()
    if (catalogo.isEmpty()) return emptyList()
    val porImagen = catalogo.associateBy { it.imageKey }
    return items.mapNotNull { item ->
        porImagen[item.imageKey]?.let { p ->
            CanastaItem(p.toProducto(), item.cantidad, item.gramos)
        }
    }
}
