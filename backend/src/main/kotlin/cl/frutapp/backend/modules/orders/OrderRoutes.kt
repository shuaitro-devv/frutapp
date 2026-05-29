package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.error.UnauthorizedException
import cl.frutapp.backend.modules.rbac.PermissionCache
import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.modules.rbac.roles
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.CreateOrderRequest
import cl.frutapp.shared.dto.TransitionRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.orderRoutes(orderService: OrderService) {
    authenticate(JWT_AUTH) {
        route("/v1/orders") {
            post {
                call.respond(
                    HttpStatusCode.Created,
                    orderService.create(call.userId(), call.receive<CreateOrderRequest>())
                )
            }
            get {
                call.respond(orderService.list(call.userId()))
            }
            get("/{id}") {
                val dto = orderService.detail(call.userId(), call.parameters["id"].orEmpty())
                val perms = PermissionCache.permissionsForRoles(call.roles())
                val actions = OrderStatus.parse(dto.status)?.let { OrderStatus.allowedActions(it, perms) } ?: emptyList()
                call.respond(dto.copy(allowedActions = actions))
            }
        }

        get("/v1/frutcoins") {
            call.respond(orderService.frutCoinsOf(call.userId()))
        }

        // Back office (futura web): requiere el permiso ESPECÍFICO del estado destino
        // (picker -> EN_PICKING, repartidor -> EN_DESPACHO/ENTREGADO, etc.). Estado inválido
        // lo maneja el servicio (422).
        post("/v1/admin/orders/{id}/transition") {
            val req = call.receive<TransitionRequest>()
            val to = OrderStatus.parse(req.toStatus)
            if (to != null) {
                val perm = OrderStatus.permissionFor(to)
                if (perm == null || !call.hasPermission(perm)) {
                    call.respond(HttpStatusCode.Forbidden)
                    return@post
                }
            }
            call.respond(orderService.transition(call.parameters["id"].orEmpty(), req))
        }
    }
}

/** userId del JWT (el `sub` es el id del usuario). */
private fun ApplicationCall.userId(): UUID {
    val sub = principal<JWTPrincipal>()?.subject ?: throw UnauthorizedException()
    return runCatching { UUID.fromString(sub) }.getOrNull() ?: throw UnauthorizedException()
}
