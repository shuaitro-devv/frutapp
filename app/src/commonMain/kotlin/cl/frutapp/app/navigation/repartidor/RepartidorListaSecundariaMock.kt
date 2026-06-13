package cl.frutapp.app.navigation.repartidor

/**
 * Mock data para los tabs 'En ruta' y 'Entregados' del repartidor. Hasta que existan los
 * endpoints del backend, son fixtures separadas de [despachosMock].
 */

/** Item del tab 'En ruta': despacho ya retirado, en camino al destino. */
data class DespachoEnRuta(
    val id: String,
    val cliente: String,
    val direccion: String,
    val sector: String,
    val kmRestantes: Double,
    val etaTexto: String, // ej. "10:25 - 10:40"
    val transito: String, // ej. "Normal", "Pesado"
    /** UUID del pedido en backend; null cuando viene del fixture mock. */
    val backendId: String? = null,
    /** URL presignada del avatar del cliente; null si el cliente nunca subio foto. */
    val avatarUrl: String? = null
)

/** Item del tab 'Entregados': despacho ya completado, historial del turno.
 *  [montoCLP] es el TOTAL del pedido entregado (lo que pago el cliente). La
 *  app no expone una "ganancia" del repartidor todavia porque el modelo de
 *  comisiones esta abierto (Modelo de negocio en curso); por eso mostramos
 *  el monto del pedido como medida de productividad del turno, no como
 *  ganancia personal. */
data class DespachoEntregado(
    val id: String,
    val cliente: String,
    val sector: String,
    val direccion: String,
    val entregadoHaceMin: Int,
    val montoCLP: Int,
    val incidencias: Int
)

internal fun despachosEnRutaMock(): List<DespachoEnRuta> = listOf(
    DespachoEnRuta(
        "#FRU-2026-672100", cliente = "Camila Torres", direccion = "Av. Italia 1234",
        sector = "Ñuñoa", kmRestantes = 1.8, etaTexto = "10:25 - 10:40", transito = "Normal"
    ),
    DespachoEnRuta(
        "#FRU-2026-672099", cliente = "Mateo Reyes", direccion = "Pedro Lira 220",
        sector = "Providencia", kmRestantes = 4.2, etaTexto = "10:55 - 11:15", transito = "Pesado"
    )
)

internal fun despachosEntregadosMock(): List<DespachoEntregado> = listOf(
    DespachoEntregado("#FRU-2026-672080", "María Fernanda Silva", "Sector Centro", "Av. Providencia 1234", entregadoHaceMin = 12, montoCLP = 5980, incidencias = 0),
    DespachoEntregado("#FRU-2026-672077", "Juan Pablo Martínez", "Sector Norte", "Las Lomas 2100", entregadoHaceMin = 45, montoCLP = 4750, incidencias = 0),
    DespachoEntregado("#FRU-2026-672070", "Carla Rodríguez", "Sector Sur", "Pedro de Valdivia 3456", entregadoHaceMin = 92, montoCLP = 3200, incidencias = 1),
    DespachoEntregado("#FRU-2026-672065", "Roberto González", "Sector Centro", "San Antonio 567", entregadoHaceMin = 138, montoCLP = 2850, incidencias = 0)
)
