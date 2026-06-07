package cl.frutapp.app.navigation.picker

/**
 * Mock data para los tabs 'En curso' y 'Listos' del picker. Hasta que existan los endpoints
 * reales, estos arreglos son fixtures separados de [pedidosColaMock] — representan pedidos
 * que YA salieron de la cola y estan en otra fase del ciclo.
 */

/** Item para el tab 'En curso': pedido tomado, en preparacion, con progreso parcial. */
data class PedidoEnCurso(
    val id: String,
    val itemsTotal: Int,
    val itemsListos: Int,
    val sector: String,
    val destino: String,
    val tiempoEnPreparacionMin: Int,
    /** UUID del pedido en backend; null cuando viene del fixture mock. */
    val backendId: String? = null
) {
    val progreso: Float get() = if (itemsTotal == 0) 0f else itemsListos.toFloat() / itemsTotal
}

/** Item para el tab 'Listos': pedido completado, esperando handoff a despacho. */
data class PedidoListo(
    val id: String,
    val items: Int,
    val sector: String,
    val destino: String,
    val terminadoHaceMin: Int,
    val picker: String,
    val incidencias: Int
)

internal fun pedidosEnCursoMock(): List<PedidoEnCurso> = listOf(
    PedidoEnCurso("#FRU-2026-458120", itemsTotal = 12, itemsListos = 5, "Sector Centro", "Café Latte", tiempoEnPreparacionMin = 9),
    PedidoEnCurso("#FRU-2026-458119", itemsTotal = 8, itemsListos = 6, "Sector Norte", "Restaurante Verde", tiempoEnPreparacionMin = 14)
)

internal fun pedidosListosMock(): List<PedidoListo> = listOf(
    PedidoListo("#FRU-2026-458110", items = 10, "Sector Centro", "Hotel Sol", terminadoHaceMin = 3, picker = "Camila R.", incidencias = 0),
    PedidoListo("#FRU-2026-458108", items = 6, "Sector Sur", "Café del Parque", terminadoHaceMin = 18, picker = "Camila R.", incidencias = 0),
    PedidoListo("#FRU-2026-458105", items = 14, "Sector Oeste", "Tienda Natural", terminadoHaceMin = 32, picker = "Camila R.", incidencias = 1),
    PedidoListo("#FRU-2026-458102", items = 4, "Sector Centro", "Oficina Central", terminadoHaceMin = 51, picker = "Camila R.", incidencias = 0)
)
