package cl.frutapp.backend.modules.baskets

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.ActualizarCanastaRequest
import cl.frutapp.shared.dto.CrearCanastaRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

/**
 * Rutas de canastas guardadas del cliente.
 *
 *  - GET    /v1/baskets         listar mis canastas
 *  - POST   /v1/baskets         crear (nombre + emoji + items)
 *  - GET    /v1/baskets/{id}    detalle
 *  - PUT    /v1/baskets/{id}    actualizar (header + opcionalmente items)
 *  - DELETE /v1/baskets/{id}    eliminar
 */
fun Route.basketRoutes(service: BasketService) {
    authenticate(JWT_AUTH) {

        get("/v1/baskets") {
            val uid = call.userId()
            call.respond(service.listar(uid))
        }

        post("/v1/baskets") {
            val uid = call.userId()
            val body = call.receive<CrearCanastaRequest>()
            call.respond(HttpStatusCode.Created, service.crear(uid, body))
        }

        get("/v1/baskets/{id}") {
            val uid = call.userId()
            val id = call.parameters["id"] ?: throw ValidationException("Falta el id.")
            call.respond(service.cargar(uid, id))
        }

        put("/v1/baskets/{id}") {
            val uid = call.userId()
            val id = call.parameters["id"] ?: throw ValidationException("Falta el id.")
            val body = call.receive<ActualizarCanastaRequest>()
            call.respond(service.actualizar(uid, id, body))
        }

        delete("/v1/baskets/{id}") {
            val uid = call.userId()
            val id = call.parameters["id"] ?: throw ValidationException("Falta el id.")
            service.eliminar(uid, id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
