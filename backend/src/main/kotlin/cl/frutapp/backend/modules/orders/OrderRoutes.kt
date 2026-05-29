package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.error.UnauthorizedException
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
                call.respond(orderService.detail(call.userId(), call.parameters["id"].orEmpty()))
            }
        }

        get("/v1/frutcoins") {
            call.respond(orderService.frutCoinsOf(call.userId()))
        }

        // Back office (futura web): solo rol OPERADOR/ADMIN. No es parte de la app cliente.
        post("/v1/admin/orders/{id}/transition") {
            if (!call.isOperator()) {
                call.respond(HttpStatusCode.Forbidden)
                return@post
            }
            call.respond(orderService.transition(call.parameters["id"].orEmpty(), call.receive<TransitionRequest>()))
        }
    }
}

/** userId del JWT (el `sub` es el id del usuario). */
private fun ApplicationCall.userId(): UUID {
    val sub = principal<JWTPrincipal>()?.subject ?: throw UnauthorizedException()
    return runCatching { UUID.fromString(sub) }.getOrNull() ?: throw UnauthorizedException()
}

private fun ApplicationCall.isOperator(): Boolean {
    val role = principal<JWTPrincipal>()?.payload?.getClaim("role")?.asString()
    return role == "OPERATOR" || role == "ADMIN"
}
