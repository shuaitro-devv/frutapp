package cl.frutapp.backend.modules.admin

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderStatus
import cl.frutapp.shared.dto.AdminOrderDetailDto
import cl.frutapp.shared.dto.AdminOrdersPageDto
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
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
     * Pedidos de un día (por defecto hoy en zona Chile) con métricas agregadas.
     * [status] opcional filtra por estado; inválido se ignora (no rompe la vista).
     */
    suspend fun list(dateParam: String?, status: String?): AdminOrdersPageDto {
        val tz = TimeZone.of("America/Santiago")
        val date = parseDateOrToday(dateParam, tz)
        val start = date.atStartOfDayIn(tz)
        val end = date.plus(1, DateTimeUnit.DAY).atStartOfDayIn(tz)
        // Estado inválido -> se ignora el filtro (en vez de 422); el panel manda solo válidos.
        val statusFilter = status?.takeIf { it.isNotBlank() }?.let { OrderStatus.parse(it)?.name }

        val list = orders.listForAdmin(start, end, statusFilter)
        val totalDia = list.sumOf { it.total }
        val ticket = if (list.isEmpty()) 0 else totalDia / list.size
        return AdminOrdersPageDto(
            orders = list,
            count = list.size,
            ticketPromedio = ticket,
            totalDia = totalDia,
            fecha = date.toString()
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

    private fun parseDateOrToday(dateParam: String?, tz: TimeZone): LocalDate {
        if (dateParam.isNullOrBlank()) return Clock.System.now().toLocalDateTime(tz).date
        return runCatching { LocalDate.parse(dateParam) }
            .getOrElse { throw ValidationException("Fecha inválida (formato esperado: YYYY-MM-DD).") }
    }
}
