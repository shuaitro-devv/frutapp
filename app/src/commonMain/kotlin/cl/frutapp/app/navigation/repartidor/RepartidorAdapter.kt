package cl.frutapp.app.navigation.repartidor

import cl.frutapp.shared.dto.StaffDispatchDetailDto
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
/** Adapta el DTO al modelo del tab "Entregados hoy" del repartidor. Calcula
 *  "hace X min" desde el assignedAt (cuando tomo el despacho). El backend
 *  no devuelve completedAt en el summary; assignedAt es proxy razonable
 *  porque las entregas tipicas son ~30-60 min. [montoCLP] usa el total del
 *  pedido (la app no expone comision personal todavia — ver doc del data
 *  class). Incidencias = 0 hasta que se cablee su seguimiento. */
internal fun StaffDispatchSummaryDto.toDespachoEntregado(): DespachoEntregado {
    val ref = runCatching { assignedAt?.let { Instant.parse(it) } }.getOrNull() ?: Clock.System.now()
    val minutos = ((Clock.System.now() - ref).inWholeMinutes).toInt().coerceAtLeast(0)
    return DespachoEntregado(
        id = numero,
        cliente = clienteNombre,
        sector = sector,
        direccion = direccion,
        entregadoHaceMin = minutos,
        montoCLP = total,
        incidencias = 0
    )
}

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
        kmDistancia = 0.0,             // sin Maps integrado, lo ocultamos en la UI (no inventar 3.0 uniforme)
        minutosEntrega = minutosDesdeCreado, // semantica: minutos transcurridos desde creado (mostrado como "Hace X min")
        prioridad = prio,
        items = itemsCount,
        unidades = itemsCount,         // approx — el detalle real lo vemos al tap
        backendId = id,
        telefono = telefono,
        avatarUrl = clienteAvatarUrl
    )
}

/** Adapter del detalle completo (cabecera + items reales) al modelo UI.
 *  Misma semantica que el summary pero con items reales para que las pantallas
 *  hijas (EnCamino, Entrega, ItemsSheet, Incidencia) no caigan al despachoPorId
 *  fixture mock. */
internal fun StaffDispatchDetailDto.toDespachoItem(): DespachoItem {
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
        kmDistancia = 0.0,
        minutosEntrega = minutosDesdeCreado,
        prioridad = prio,
        items = items.size,
        unidades = items.size,
        backendId = id,
        telefono = telefono,
        avatarUrl = clienteAvatarUrl
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
    backendId = id,
    avatarUrl = clienteAvatarUrl
)
