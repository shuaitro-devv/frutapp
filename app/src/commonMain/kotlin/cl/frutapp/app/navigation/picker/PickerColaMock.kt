package cl.frutapp.app.navigation.picker

/**
 * Mock data para la pantalla "Cola de pedidos" del picker. Vive aca hasta que existan los
 * endpoints reales del backend (GET /v1/picker/orders). Cuando se cablee a la API, este
 * archivo se elimina y los pedidos vienen del [cl.frutapp.app.data.remote.PickerApi] futuro.
 */

enum class PrioridadCola(val label: String) {
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja")
}

/**
 * Pedido en la cola del picker. Refleja lo minimo que necesita la card del mockup picker-01:
 * id legible, conteo de items, peso total, tiempo desde que llego al picker, sector/destino,
 * y prioridad calculada (por tipo de cliente, antiguedad, o flag del operador).
 */
data class PedidoColaItem(
    val id: String,
    val items: Int,
    val pesoKg: Double,
    val minutosEspera: Int,
    val sector: String,
    val destino: String,
    val prioridad: PrioridadCola
) {
    val urgente: Boolean get() = minutosEspera > 15

    /** Antigüedad legible para la card: "hace 8 min" / "hace 1 h 5 min". */
    fun antiguedadHumano(): String {
        val h = minutosEspera / 60
        val m = minutosEspera % 60
        return when {
            h > 0 && m > 0 -> "hace ${h} h ${m} min"
            h > 0 -> "hace ${h} h"
            else -> "hace ${m} min"
        }
    }
}

/** Fixture estable para mostrar el mockup en vivo. Los IDs son legibles tipo "#FRU-2026-XXXXXX". */
internal fun pedidosColaMock(): List<PedidoColaItem> = listOf(
    PedidoColaItem(
        id = "#FRU-2026-458231",
        items = 12,
        pesoKg = 4.2,
        minutosEspera = 18,
        sector = "Sector Norte",
        destino = "Restaurante Verde",
        prioridad = PrioridadCola.ALTA
    ),
    PedidoColaItem(
        id = "#FRU-2026-458232",
        items = 7,
        pesoKg = 2.4,
        minutosEspera = 12,
        sector = "Sector Centro",
        destino = "Hotel Sol",
        prioridad = PrioridadCola.ALTA
    ),
    PedidoColaItem(
        id = "#FRU-2026-458233",
        items = 5,
        pesoKg = 1.8,
        minutosEspera = 28,
        sector = "Sector Sur",
        destino = "Café del Parque",
        prioridad = PrioridadCola.MEDIA
    ),
    PedidoColaItem(
        id = "#FRU-2026-458234",
        items = 9,
        pesoKg = 3.1,
        minutosEspera = 35,
        sector = "Sector Oeste",
        destino = "Tienda Natural",
        prioridad = PrioridadCola.BAJA
    ),
    PedidoColaItem(
        id = "#FRU-2026-458235",
        items = 3,
        pesoKg = 1.0,
        minutosEspera = 42,
        sector = "Sector Centro",
        destino = "Oficina Central",
        prioridad = PrioridadCola.BAJA
    )
)
