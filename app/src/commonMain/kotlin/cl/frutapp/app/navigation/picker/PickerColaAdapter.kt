package cl.frutapp.app.navigation.picker

import cl.frutapp.shared.dto.StaffOrderDetailDto
import cl.frutapp.shared.dto.StaffOrderSummaryDto
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Adapta el DTO del backend ([StaffOrderSummaryDto]) al modelo de UI de la cola
 * del picker ([PedidoColaItem]). El backend manda lo minimo (id, status, items,
 * cliente, sector, createdAt); el UI deriva el resto:
 *
 *  - minutosEspera: diff entre `createdAt` del backend y `now()` local.
 *  - prioridad: heuristica por antiguedad (>15min ALTA, >5min MEDIA, sino BAJA).
 *  - pesoKg: no disponible en summary; el picker lo ve en el detalle. Para la
 *    card mostramos 0 y dejamos que el componente decida si ocultarlo.
 *
 * Cuando ampliemos el StaffOrderSummaryDto con peso total estimado, lo agregamos
 * aca sin tocar la UI.
 */
internal fun StaffOrderSummaryDto.toPedidoColaItem(): PedidoColaItem {
    val creado = runCatching { Instant.parse(createdAt) }.getOrNull() ?: Clock.System.now()
    val minutos = ((Clock.System.now() - creado).inWholeMinutes).toInt().coerceAtLeast(0)
    val prioridad = when {
        minutos > 15 -> PrioridadCola.ALTA
        minutos > 5 -> PrioridadCola.MEDIA
        else -> PrioridadCola.BAJA
    }
    return PedidoColaItem(
        id = numero,
        items = itemsCount,
        pesoKg = 0.0,
        minutosEspera = minutos,
        sector = sector,
        destino = "Pedido de $clienteNombre",
        prioridad = prioridad,
        backendId = id
    )
}

/** Adapta el DTO al modelo del tab "En curso" (pedidos EN_PICKING que tomó este picker).
 *  El backend manda el assignedAt (cuando lo tomamos); calculamos minutos desde ahi.
 *  El progreso de items resueltos hoy no viene del backend (el estado per-item es
 *  Nivel 2) — devolvemos 0 de N para que la UI muestre "0/N en proceso"; cuando
 *  cableemos items individuales, este valor reflejara el avance real. */
/** Adapta el DTO al modelo del tab "Listos hoy" del picker. Calcula "hace X min"
 *  desde el assignedAt (cuando lo tomo). El backend del summary NO trae el
 *  updatedAt cuando se completo; usamos assignedAt como proxy razonable porque
 *  son normalmente pocos minutos. Cuando ampliemos el DTO con completedAt, lo
 *  enchufamos aca. Incidencias = 0 hasta que tengamos su seguimiento; el campo
 *  queda para el cableado futuro. */
internal fun StaffOrderSummaryDto.toPedidoListo(): PedidoListo {
    val ref = runCatching { assignedAt?.let { Instant.parse(it) } }.getOrNull() ?: Clock.System.now()
    val minutos = ((Clock.System.now() - ref).inWholeMinutes).toInt().coerceAtLeast(0)
    return PedidoListo(
        id = numero,
        items = itemsCount,
        sector = sector,
        destino = "Pedido de $clienteNombre",
        terminadoHaceMin = minutos,
        picker = clienteNombre,
        incidencias = 0
    )
}

internal fun StaffOrderSummaryDto.toPedidoEnCurso(): PedidoEnCurso {
    val tomadoEn = runCatching { assignedAt?.let { Instant.parse(it) } }.getOrNull() ?: Clock.System.now()
    val minutosEnPrep = ((Clock.System.now() - tomadoEn).inWholeMinutes).toInt().coerceAtLeast(0)
    return PedidoEnCurso(
        id = numero,
        itemsTotal = itemsCount,
        itemsListos = 0,
        sector = sector,
        destino = "Pedido de $clienteNombre",
        tiempoEnPreparacionMin = minutosEnPrep,
        backendId = id
    )
}

/** Adapta el detalle del backend al modelo de UI del picklist. Los campos pasillo/estante
 *  no vienen del backend (catalogo no los tiene aun) — los dejamos vacios para no
 *  mentir; el catalogo real eventualmente trae la ubicacion fisica del producto y
 *  enchufamos ahi. Idem tiempoEstimado: 15 min default razonable hasta tener historico. */
internal fun StaffOrderDetailDto.toPicklistData(): PicklistData = PicklistData(
    pedidoId = numero,
    sector = sector,
    destino = "Pedido de $clienteNombre",
    tiempoEstimadoMin = 15,
    items = items.map { item ->
        ItemPicklist(
            numero = item.numero,
            nombre = item.nombre,
            cantidad = item.cantidad,
            unidad = item.unidad,
            pasillo = "—",
            estante = "—",
            pesoVariable = item.pesoVariable,
            emoji = item.emoji,
            estado = EstadoItem.PENDIENTE,
            backendId = item.id,
            imageKey = item.imageKey,
            backendProductId = item.productId
        )
    },
    tomadoEnIso = assignedAt
)
