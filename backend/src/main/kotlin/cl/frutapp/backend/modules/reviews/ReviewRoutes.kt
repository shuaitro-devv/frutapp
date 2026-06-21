package cl.frutapp.backend.modules.reviews

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.CrearResenaRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import java.util.UUID

/**
 * Rutas de resenas de producto.
 *
 *  - POST /v1/products/{productId}/reviews          (auth)   upsert mi resena
 *  - GET  /v1/products/{productId}/reviews                   listar (publico)
 *  - GET  /v1/products/{productId}/reviews/mine     (auth)   devuelve mi resena o 404
 *
 * El listado es publico (no requiere login) porque el detalle del producto
 * en el catalogo lo consulta sin sesion (preview pre-checkout); el contenido
 * es agregado de los clientes, sin datos sensibles.
 */
fun Route.reviewRoutes(service: ReviewService) {

    fun parseProductId(call: io.ktor.server.application.ApplicationCall): UUID =
        call.parameters["productId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: throw ValidationException("productId inválido.")

    // GET publico — sin authenticate { }.
    get("/v1/products/{productId}/reviews") {
        val productId = parseProductId(call)
        call.respond(service.listar(productId))
    }

    authenticate(JWT_AUTH) {

        post("/v1/products/{productId}/reviews") {
            val uid = call.userId()
            val productId = parseProductId(call)
            val body = call.receive<CrearResenaRequest>()
            val dto = service.crearOActualizar(
                userId = uid,
                productId = productId,
                estrellas = body.estrellas,
                texto = body.texto,
            )
            call.respond(HttpStatusCode.Created, dto)
        }

        get("/v1/products/{productId}/reviews/mine") {
            val uid = call.userId()
            val productId = parseProductId(call)
            val dto = service.miResena(uid, productId)
                ?: run { call.respond(HttpStatusCode.NotFound); return@get }
            call.respond(dto)
        }
    }
}
