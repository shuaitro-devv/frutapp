package cl.frutapp.app.navigation.repartidor

/**
 * Mock data del flujo del repartidor (cola, detalle, ruta, entrega, incidencia, saldo).
 * Vive aca hasta que existan los endpoints reales en el backend. Cuando se cablee, este
 * archivo se reemplaza por respuestas del [cl.frutapp.app.data.remote.RepartidorApi].
 */

enum class PrioridadDespacho(val label: String) {
    PARA_RETIRO("Para retiro"),
    ALTA("Alta"),
    MEDIA("Media"),
    BAJA("Baja")
}

data class DespachoItem(
    val id: String,
    val cliente: String,
    val sector: String,
    val direccion: String,
    val kmDistancia: Double,
    val minutosEntrega: Int,
    val prioridad: PrioridadDespacho,
    val items: Int,
    val unidades: Int
) {
    val urgente: Boolean get() = minutosEntrega < 20
    fun tiempoEntregaHumano(): String {
        val h = minutosEntrega / 60
        val m = minutosEntrega % 60
        return when {
            h > 0 && m > 0 -> "Entrega en ${h} h ${m} min"
            h > 0 -> "Entrega en ${h} h"
            else -> "Entrega en ${m} min"
        }
    }
}

/** Busca un despacho por ID en la cola; si no aparece (caso: vino de las listas
 *  secundarias 'En ruta' o 'Entregados' con sus propios IDs mock), devuelve el primer
 *  item de la cola como fallback razonable para que las pantallas de detalle no exploten
 *  con NoSuchElementException. Cuando exista el backend, este lookup pasa a ser un GET. */
internal fun despachoPorId(id: String): DespachoItem =
    despachosMock().firstOrNull { it.id == id } ?: despachosMock().first()

internal fun despachosMock(): List<DespachoItem> = listOf(
    DespachoItem(
        id = "#FRU-2026-672341", cliente = "María Fernanda Silva", sector = "Sector Centro",
        direccion = "Av. Providencia 1234", kmDistancia = 2.4, minutosEntrega = 18,
        prioridad = PrioridadDespacho.ALTA, items = 12, unidades = 42
    ),
    DespachoItem(
        id = "#FRU-2026-672342", cliente = "Juan Pablo Martínez", sector = "Sector Norte",
        direccion = "Las Lomas 2100", kmDistancia = 4.1, minutosEntrega = 27,
        prioridad = PrioridadDespacho.MEDIA, items = 8, unidades = 24
    ),
    DespachoItem(
        id = "#FRU-2026-672343", cliente = "Carla Rodríguez", sector = "Sector Sur",
        direccion = "Pedro de Valdivia 3456", kmDistancia = 5.5, minutosEntrega = 45,
        prioridad = PrioridadDespacho.MEDIA, items = 6, unidades = 18
    ),
    DespachoItem(
        id = "#FRU-2026-672344", cliente = "Supermercado El Bosque", sector = "Sector El Bosque",
        direccion = "Concha y Toro 987", kmDistancia = 7.8, minutosEntrega = 62,
        prioridad = PrioridadDespacho.BAJA, items = 30, unidades = 110
    ),
    DespachoItem(
        id = "#FRU-2026-672345", cliente = "Roberto González", sector = "Sector Centro",
        direccion = "San Antonio 567", kmDistancia = 3.2, minutosEntrega = 75,
        prioridad = PrioridadDespacho.BAJA, items = 5, unidades = 14
    )
)

data class TransaccionSaldo(
    val tipo: TipoTransaccion,
    val descripcion: String,
    val cuando: String,
    val monto: Int, // CLP, positivo o negativo
    val estado: String
)

enum class TipoTransaccion { ENTREGA, COMISION, TRANSFERENCIA }

internal fun saldoResumenMock() = SaldoResumen(
    disponible = 48_750,
    actualizacion = "Hoy 09:41",
    enTransito = 23_400,
    transferenciasEnTransito = 2,
    proximoPagoFecha = "Jue, 16 may",
    proximoPagoEstimado = 23_400,
    entregasSemana = 42,
    gananciasBrutas = 72_150,
    deducciones = 2_700,
    totalNeto = 69_450,
    transacciones = listOf(
        TransaccionSaldo(TipoTransaccion.ENTREGA, "Entrega #FRU-2026-0012345", "15 may · 10:32", 5980, "Entregado"),
        TransaccionSaldo(TipoTransaccion.ENTREGA, "Entrega #FRU-2026-0009118", "15 may · 09:16", 4750, "Entregado"),
        TransaccionSaldo(TipoTransaccion.COMISION, "Comisión de la plataforma", "15 may · 00:00", -1350, "Aplicada"),
        TransaccionSaldo(TipoTransaccion.TRANSFERENCIA, "Transferencia a tu cuenta", "14 may · 18:45", -46_000, "Procesada")
    )
)

data class SaldoResumen(
    val disponible: Int,
    val actualizacion: String,
    val enTransito: Int,
    val transferenciasEnTransito: Int,
    val proximoPagoFecha: String,
    val proximoPagoEstimado: Int,
    val entregasSemana: Int,
    val gananciasBrutas: Int,
    val deducciones: Int,
    val totalNeto: Int,
    val transacciones: List<TransaccionSaldo>
)

internal fun formatoClp(monto: Int): String {
    val abs = kotlin.math.abs(monto)
    val sb = StringBuilder()
    val s = abs.toString()
    for (i in s.indices) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
        sb.append(s[i])
    }
    return if (monto < 0) "-$$sb" else "$$sb"
}
