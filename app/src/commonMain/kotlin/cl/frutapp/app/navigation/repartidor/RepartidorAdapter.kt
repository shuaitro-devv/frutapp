package cl.frutapp.app.navigation.repartidor

import cl.frutapp.shared.dto.StaffDispatchSummaryDto
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Adapta el DTO del backend (StaffDispatchSummaryDto) al modelo UI del
 * repartidor (DespachoItem). El backend manda los datos reales (cliente,
 * direccion, telefono opcional, sector, items); el UI deriva el resto:
 *  - kmDistancia: heuristica simple porque no tenemos geocoding; usamos un
 *    valor placeholder hasta integrar Google Maps en otra iteracion.
 *  - minutosEntrega: estimacion default de 30 min para que la UI muestre
 *    algo plausible. Cuando integremos ruteo real esto sale del API.
 *  - prioridad: derivada del tiempo desde que el pedido se creo
 *    (mientras mas viejo, mas prioritario para no acumular SLA).
 */
internal fun StaffDispatchSummaryDto.toDespachoItem(): DespachoItem {
    val creado = runCatching { Instant.parse(createdAt) }.getOrNull() ?: Clock.System.now()
    val minutosDesdeCreado = ((Clock.System.now() - creado).inWholeMinutes).toInt().coerceAtLeast(0)
    val prio = when {
        minutosDesdeCreado > 60 -> PrioridadDespacho.ALTA
        minutosDesdeCreado > 20 -> PrioridadDespacho.MEDIA
        else -> PrioridadDespacho.BAJA
    }
    return DespachoItem(
        id = numero,
        cliente = clienteNombre,
        sector = sector,
        direccion = direccion,
        kmDistancia = 3.0,            // placeholder hasta integrar Maps
        minutosEntrega = 30,           // placeholder hasta integrar ruteo
        prioridad = prio,
        items = itemsCount,
        unidades = itemsCount,         // approx — el detalle real lo vemos al tap
        backendId = id,
        telefono = telefono
    )
}

/** Adapta el summary del backend al modelo "en ruta" (mis despachos EN_DESPACHO). */
internal fun StaffDispatchSummaryDto.toDespachoEnRuta(): DespachoEnRuta = DespachoEnRuta(
    id = numero,
    cliente = clienteNombre,
    direccion = direccion,
    sector = sector,
    kmRestantes = 3.0,  // placeholder hasta integrar Maps
    etaTexto = "Por confirmar",
    transito = "Normal",
    backendId = id
)
