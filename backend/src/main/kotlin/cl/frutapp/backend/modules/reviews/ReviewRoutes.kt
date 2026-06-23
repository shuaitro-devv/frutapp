package cl.frutapp.backend.modules.reviews

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.core.readBytes
import java.util.UUID

/**
 * Rutas de resenas de producto.
 *
 *  - POST /v1/products/{productId}/reviews          (auth, multipart)   upsert mi resena
 *  - GET  /v1/products/{productId}/reviews                              listar (publico)
 *  - GET  /v1/products/{productId}/reviews/mine     (auth)              devuelve mi resena o 404
 *
 * El POST es multipart (mismo patron que chat/evidencia):
 *   - estrellas    (FormItem, requerido, 1..5)
 *   - texto        (FormItem, opcional)
 *   - removerImagen (FormItem "true"/"false", opcional, default false)
 *   - archivo      (FileItem, opcional, imagen JPEG/PNG <=5 MB)
 *
 * El GET listar es publico (no requiere login) porque el detalle del producto
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
            var estrellas: Int? = null
            var texto = ""
            var removerImagen = false
            var bytes: ByteArray? = null
            var contentType: String? = null
            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        contentType = part.contentType?.toString() ?: "application/octet-stream"
                        bytes = part.provider().readBytes()
                    }
                    is PartData.FormItem -> when (part.name) {
                        "estrellas" -> estrellas = part.value.toIntOrNull()
                        "texto" -> texto = part.value
                        "removerImagen" -> removerImagen = part.value.equals("true", ignoreCase = true)
                    }
                    else -> Unit
                }
                part.dispose()
            }
            val n = estrellas ?: throw ValidationException("Falta el campo 'estrellas'.")
            val dto = service.crearOActualizar(
                userId = uid,
                productId = productId,
                estrellas = n,
                texto = texto,
                imagenBytes = bytes,
                imagenContentType = contentType,
                removerImagen = removerImagen,
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
