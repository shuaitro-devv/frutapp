package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf

enum class OrderEstado(val label: String) {
    EN_CURSO("En curso"),
    COMPLETADO("Completado"),
    CANCELADO("Cancelado")
}

data class Order(
    val numero: String,
    val fecha: String,
    val total: Int,
    val estado: OrderEstado,
    val direccion: String,
    val entrega: String
)

/**
 * Pedidos del usuario en memoria (cliente). Sembrado con un pedido demo y se le suman
 * los pedidos hechos en la sesión al pagar. Cuando exista el backend de órdenes, esto
 * se reemplaza por la API (y los pedidos persisten entre sesiones).
 */
object OrdersStore {
    val pedidos = mutableStateListOf(
        Order(
            numero = "#FRU-2026-100234",
            fecha = "Hace 3 días",
            total = 12450,
            estado = OrderEstado.COMPLETADO,
            direccion = "Av. Siempre Viva 742, Santiago",
            entrega = "Entregado"
        )
    )

    /** Agrega un pedido nuevo arriba (el más reciente primero). */
    fun add(order: Order) {
        pedidos.add(0, order)
    }
}
