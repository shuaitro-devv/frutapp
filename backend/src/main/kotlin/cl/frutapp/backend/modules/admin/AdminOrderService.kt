package cl.frutapp.backend.modules.admin

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderStatus
import cl.frutapp.shared.dto.AdminOrderDetailDto
import cl.frutapp.shared.dto.AdminOrdersPageDto
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

/**
 * Back office: consultas de pedidos a nivel global (todos los clientes). El
 * cálculo de ticket/total vive acá; el front solo renderiza. Gating por permiso
 * (`order:read_all`) lo hace la ruta antes de llamar a este servicio.
 */
class AdminOrderService(private val orders: OrderRepository) {

    /**
     * Pedidos para la cola del back office. SIN fecha -> ventana reciente (últimos
     * [RANGO_RECIENTE_DIAS] días) para operar (un pedido de ayer EN_PICKING necesita
     * atención hoy). CON fecha -> ese día puntual. [status] opcional filtra por
     * estado; inválido se ignora (no rompe la vista). Métricas sobre lo devuelto.
     */
    suspend fun list(dateParam: String?, status: String?): AdminOrdersPageDto {
        val tz = TimeZone.of("America/Santiago")
        // Estado inválido -> se ignora el filtro (en vez de 422); el panel manda solo válidos.
        val statusFilter = status?.takeIf { it.isNotBlank() }?.let { OrderStatus.parse(it)?.name }
        val hoy = Clock.System.now().toLocalDateTime(tz).date
        val (start, end, fecha) = adminOrdersRange(dateParam, hoy, tz)

        val list = orders.listForAdmin(start, end, statusFilter)
        val totalDia = list.sumOf { it.total }
        val ticket = if (list.isEmpty()) 0 else totalDia / list.size
        return AdminOrdersPageDto(
            orders = list,
            count = list.size,
            ticketPromedio = ticket,
            totalDia = totalDia,
            fecha = fecha
        )
    }

    /**
     * Detalle de un pedido + datos del cliente. NO calcula `allowedActions`: eso lo
     * resuelve la ruta con los permisos del llamante (estado × permisos).
     */
    suspend fun detail(idStr: String): AdminOrderDetailDto {
        val id = runCatching { UUID.fromString(idStr) }.getOrNull()
            ?: throw ValidationException("Id inválido.")
        val order = orders.findById(id) ?: throw NotFoundException("Pedido no encontrado.")
        val cliente = orders.findClienteOf(id)
        return AdminOrderDetailDto(
            order = order,
            clienteNombre = cliente?.nombre ?: "Cliente",
            clienteEmail = cliente?.email ?: "",
            clienteTelefono = cliente?.telefono
        )
    }

}

/** Ventana por defecto (sin fecha) para la cola operativa reciente. */
internal const val RANGO_RECIENTE_DIAS = 14

/**
 * Ventana [start, end) (half-open) + etiqueta de fecha para la cola del back office.
 * Sin [dateParam] -> últimos [RANGO_RECIENTE_DIAS] días hasta hoy; con fecha -> ese día.
 * Pura y determinista (recibe [hoy]) para poder testearla sin reloj ni BD.
 */
internal fun adminOrdersRange(dateParam: String?, hoy: LocalDate, tz: TimeZone): Triple<Instant, Instant, String> {
    return if (dateParam.isNullOrBlank()) {
        val desde = hoy.minus(RANGO_RECIENTE_DIAS, DateTimeUnit.DAY)
        Triple(desde.atStartOfDayIn(tz), hoy.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz), hoy.toString())
    } else {
        val date = runCatching { LocalDate.parse(dateParam) }
            .getOrElse { throw ValidationException("Fecha inválida (formato esperado: YYYY-MM-DD).") }
        Triple(date.atStartOfDayIn(tz), date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz), date.toString())
    }
}
