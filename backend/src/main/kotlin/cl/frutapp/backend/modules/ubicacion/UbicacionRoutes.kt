package cl.frutapp.backend.modules.ubicacion

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.ReportarUbicacionRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/** Endpoints de tracking de ubicacion del repartidor. */
fun Route.ubicacionRoutes(service: UbicacionService) {
    authenticate(JWT_AUTH) {

        // POST /v1/staff/dispatches/{orderId}/ubicacion
        // Repartidor reporta su posicion actual. Gated por `order:dispatch`.
        post("/v1/staff/dispatches/{orderId}/ubicacion") {
            if (!call.hasPermission("order:dispatch")) {
                call.respond(HttpStatusCode.Forbidden); return@post
            }
            val uid = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId invalido.")
            val body = call.receive<ReportarUbicacionRequest>()
            service.reportar(uid, orderId, body.lat, body.lng)
            call.respond(HttpStatusCode.NoContent)
        }

        // GET /v1/orders/{orderId}/ubicacion
        // Cliente consulta la ubicacion del repartidor de su pedido.
        get("/v1/orders/{orderId}/ubicacion") {
            val uid = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId invalido.")
            val esDelCliente = dbQuery {
                OrdersTable.selectAll().where {
                    (OrdersTable.id eq orderId) and (OrdersTable.userId eq uid)
                }.any()
            }
            if (!esDelCliente) {
                call.respond(HttpStatusCode.NotFound); return@get
            }
            val ubic = service.paraCliente(orderId)
            if (ubic == null) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respond(ubic)
            }
        }
    }
}
