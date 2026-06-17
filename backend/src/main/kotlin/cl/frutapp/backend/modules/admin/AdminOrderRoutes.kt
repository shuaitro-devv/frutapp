package cl.frutapp.backend.modules.admin

import cl.frutapp.backend.modules.orders.OrderStatus
import cl.frutapp.backend.modules.rbac.PermissionCache
import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.modules.rbac.roles
import cl.frutapp.backend.plugins.JWT_AUTH
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Back office: lectura global de pedidos. Gated por `order:read_all` (admin/soporte;
 * el cliente NO lo tiene, aunque tenga `order:read`). La transición de estado vive
 * en OrderRoutes (`POST /v1/admin/orders/{id}/transition`).
 */
fun Route.adminOrderRoutes(service: AdminOrderService) {
    authenticate(JWT_AUTH) {
        route("/v1/admin/orders") {

            // GET /v1/admin/orders?date=YYYY-MM-DD&status=PAGADO -> pedidos del día + ticket.
            get {
                if (!call.hasPermission("order:read_all")) {
                    call.respond(HttpStatusCode.Forbidden); return@get
                }
                val date = call.request.queryParameters["date"]
                val status = call.request.queryParameters["status"]
                call.respond(service.list(date, status))
            }

            // GET /v1/admin/orders/{id} -> detalle + cliente + allowedActions (estado × permisos).
            get("/{id}") {
                if (!call.hasPermission("order:read_all")) {
                    call.respond(HttpStatusCode.Forbidden); return@get
                }
                val dto = service.detail(call.parameters["id"].orEmpty())
                val perms = PermissionCache.permissionsForRoles(call.roles())
                val actions = OrderStatus.parse(dto.order.status)
                    ?.let { OrderStatus.allowedActions(it, perms) } ?: emptyList()
                call.respond(dto.copy(order = dto.order.copy(allowedActions = actions)))
            }
        }
    }
}
