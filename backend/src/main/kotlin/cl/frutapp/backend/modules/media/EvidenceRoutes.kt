package cl.frutapp.backend.modules.media

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.eventContext
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.backend.db.dbQuery
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.utils.io.core.readBytes
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Rutas de evidencia visual:
 *
 *  - POST /v1/staff/orders/{orderId}/items/{itemId}/evidence (multipart)
 *      Picker sube una foto del item al completarlo. Requiere `order:pick`.
 *      Multipart con un FileItem (la foto) y opcionalmente un FormItem
 *      "comentario" con texto.
 *
 *  - GET /v1/orders/{orderId}/evidence
 *      Cliente lista todas las evidencias del pedido (agrupadas por item en
 *      el frontend). Solo el dueño del pedido las ve.
 */
fun Route.evidenceRoutes(service: EvidenceService) {
    authenticate(JWT_AUTH) {

        post("/v1/staff/orders/{orderId}/items/{itemId}/evidence") {
            if (!call.hasPermission("order:pick")) {
                call.respond(HttpStatusCode.Forbidden); return@post
            }
            val pickerId = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId inválido.")
            val itemId = call.parameters["itemId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("itemId inválido.")
            var bytes: ByteArray? = null
            var contentType: String? = null
            var comentario: String? = null
            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        contentType = part.contentType?.toString() ?: "application/octet-stream"
                        bytes = part.provider().readBytes()
                    }
                    is PartData.FormItem -> {
                        if (part.name == "comentario") comentario = part.value.takeIf { it.isNotBlank() }
                    }
                    else -> Unit
                }
                part.dispose()
            }
            val b = bytes ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Falta el archivo"))
                return@post
            }
            val dto = service.uploadAsPicker(
                pickerId = pickerId,
                orderId = orderId,
                orderItemId = itemId,
                bytes = b,
                contentType = contentType ?: "application/octet-stream",
                comentario = comentario,
                context = call.eventContext()
            )
            call.respond(HttpStatusCode.Created, cl.frutapp.shared.dto.UploadEvidenceResponse(evidencia = dto))
        }

        // Repartidor sube UNA foto del paquete al confirmar la entrega.
        // Gated por `order:dispatch` (mismo permiso con el que toma/reporta).
        // El service valida que el pedido este EN_DESPACHO y asignado a este
        // repartidor.
        post("/v1/staff/dispatches/{orderId}/evidence") {
            if (!call.hasPermission("order:dispatch")) {
                call.respond(HttpStatusCode.Forbidden); return@post
            }
            val repartidorId = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId inválido.")
            var bytes: ByteArray? = null
            var contentType: String? = null
            var comentario: String? = null
            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        contentType = part.contentType?.toString() ?: "application/octet-stream"
                        bytes = part.provider().readBytes()
                    }
                    is PartData.FormItem -> {
                        if (part.name == "comentario") comentario = part.value.takeIf { it.isNotBlank() }
                    }
                    else -> Unit
                }
                part.dispose()
            }
            val b = bytes ?: run {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Falta el archivo"))
                return@post
            }
            val dto = service.uploadAsRepartidor(
                repartidorId = repartidorId,
                orderId = orderId,
                bytes = b,
                contentType = contentType ?: "application/octet-stream",
                comentario = comentario,
                context = call.eventContext()
            )
            call.respond(HttpStatusCode.Created, cl.frutapp.shared.dto.UploadEvidenceResponse(evidencia = dto))
        }

        // El repartidor borra una foto de entrega que subio antes de confirmar
        // la entrega (previsualiza y decide reemplazarla). Mismo gate del POST.
        delete("/v1/staff/dispatches/{orderId}/evidence/{evidenceId}") {
            if (!call.hasPermission("order:dispatch")) {
                call.respond(HttpStatusCode.Forbidden); return@delete
            }
            val repartidorId = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId inválido.")
            val evidenceId = call.parameters["evidenceId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("evidenceId inválido.")
            service.deleteAsRepartidor(repartidorId, orderId, evidenceId, call.eventContext())
            call.respond(HttpStatusCode.NoContent)
        }

        get("/v1/orders/{orderId}/evidence") {
            val clienteId = call.userId()
            val orderId = call.parameters["orderId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: throw ValidationException("orderId inválido.")
            val esDelCliente = dbQuery {
                OrdersTable.selectAll().where {
                    (OrdersTable.id eq orderId) and (OrdersTable.userId eq clienteId)
                }.any()
            }
            if (!esDelCliente) {
                call.respond(HttpStatusCode.NotFound); return@get
            }
            call.respond(service.listByOrder(orderId))
        }
    }
}
