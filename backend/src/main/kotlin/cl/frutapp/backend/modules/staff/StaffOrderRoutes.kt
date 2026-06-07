package cl.frutapp.backend.modules.staff

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.eventContext
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.plugins.JWT_AUTH
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

/**
 * Rutas del staff (picker/repartidor): cola libre, mis en curso, tomar, devolver,
 * completar. Todas requieren el permiso `order:pick` (picker) y aplican RBAC
 * antes de la accion. La auditoria queda en user_event vía StaffOrderService.
 */
fun Route.staffOrderRoutes(staffOrders: StaffOrderService) {
    authenticate(JWT_AUTH) {
        route("/v1/staff/orders") {

            // GET /v1/staff/orders?status=cola         -> cola libre de mi location
            // GET /v1/staff/orders?status=en_curso     -> mis pedidos en EN_PICKING
            get {
                if (!call.hasPermission("order:pick")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val pickerId = call.userId()
                val statusFilter = call.request.queryParameters["status"].orEmpty()
                val result = when (statusFilter) {
                    "cola" -> staffOrders.colaPicker(pickerId)
                    "en_curso" -> staffOrders.enCursoPicker(pickerId)
                    else -> staffOrders.colaPicker(pickerId)
                }
                call.respond(result)
            }

            // GET /v1/staff/orders/{id} -> detalle completo (cabecera + items)
            get("/{id}") {
                if (!call.hasPermission("order:pick")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val pickerId = call.userId()
                val orderId = call.orderIdParam()
                call.respond(staffOrders.detalle(pickerId, orderId))
            }

            // POST /v1/staff/orders/{id}/take
            post("/{id}/take") {
                if (!call.hasPermission("order:pick")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
                val pickerId = call.userId()
                val orderId = call.orderIdParam()
                val result = staffOrders.take(pickerId, orderId, call.eventContext())
                if (result.ok) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.Conflict, result)
                }
            }

            // POST /v1/staff/orders/{id}/release       -> devolver a la cola
            post("/{id}/release") {
                if (!call.hasPermission("order:pick")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
                val pickerId = call.userId()
                val orderId = call.orderIdParam()
                staffOrders.release(pickerId, orderId, call.eventContext())
                call.respond(HttpStatusCode.NoContent)
            }

            // POST /v1/staff/orders/{id}/complete      -> marcar STOCK_CONFIRMADO
            post("/{id}/complete") {
                if (!call.hasPermission("order:confirm_stock")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
                val pickerId = call.userId()
                val orderId = call.orderIdParam()
                staffOrders.complete(pickerId, orderId, call.eventContext())
                call.respond(HttpStatusCode.NoContent)
            }

            // ========== Flujo REPARTIDOR (Nivel 3) ==========
            // GET /v1/staff/orders/dispatch?status=cola      -> pedidos STOCK_CONFIRMADO
            // GET /v1/staff/orders/dispatch?status=en_ruta   -> mis EN_DESPACHO
            get("/dispatch") {
                if (!call.hasPermission("order:dispatch")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val repartidorId = call.userId()
                val statusFilter = call.request.queryParameters["status"].orEmpty()
                val result = when (statusFilter) {
                    "cola" -> staffOrders.colaDispatch(repartidorId)
                    "en_ruta" -> staffOrders.enRutaDispatch(repartidorId)
                    else -> staffOrders.colaDispatch(repartidorId)
                }
                call.respond(result)
            }

            // GET /v1/staff/orders/dispatch/{id} -> detalle con direccion + telefono
            get("/dispatch/{id}") {
                if (!call.hasPermission("order:dispatch")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@get
                }
                val repartidorId = call.userId()
                val orderId = call.orderIdParam()
                call.respond(staffOrders.detalleDispatch(repartidorId, orderId))
            }

            // POST /v1/staff/orders/dispatch/{id}/take     -> tomar despacho (status EN_DESPACHO)
            post("/dispatch/{id}/take") {
                if (!call.hasPermission("order:dispatch")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
                val repartidorId = call.userId()
                val orderId = call.orderIdParam()
                val result = staffOrders.takeDispatch(repartidorId, orderId, call.eventContext())
                if (result.ok) call.respond(HttpStatusCode.OK, result)
                else call.respond(HttpStatusCode.Conflict, result)
            }

            // POST /v1/staff/orders/dispatch/{id}/delivered -> ENTREGADO
            post("/dispatch/{id}/delivered") {
                if (!call.hasPermission("order:deliver")) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
                val repartidorId = call.userId()
                val orderId = call.orderIdParam()
                staffOrders.deliveredDispatch(repartidorId, orderId, call.eventContext())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun ApplicationCall.orderIdParam(): UUID {
    val raw = parameters["id"].orEmpty()
    return runCatching { UUID.fromString(raw) }.getOrNull()
        ?: throw ValidationException("orderId inválido")
}

