package cl.frutapp.backend.modules.media

import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.core.readBytes
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
private data class AvatarUploadResponse(val avatarUrl: String)

fun Route.avatarRoutes(service: AvatarService) {
    authenticate(JWT_AUTH) {
        route("/v1/me/avatar") {
            // Subida de avatar via multipart. Campo de archivo: "archivo" (mismo nombre
            // que polizapp para reusar el SelectorImagenes). Devuelve la URL presignada
            // para que el cliente la cachee de inmediato sin esperar al proximo
            // GET /v1/auth/me.
            post {
                val uid = call.userId()
                var url: String? = null
                call.receiveMultipart().forEachPart { part ->
                    if (part is PartData.FileItem) {
                        val contentType = part.contentType?.toString() ?: "application/octet-stream"
                        val bytes = part.provider().readBytes()
                        url = service.upload(uid, bytes, contentType)
                    }
                    part.dispose()
                }
                val result = url
                if (result == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Falta el archivo"))
                } else {
                    call.respond(HttpStatusCode.Created, AvatarUploadResponse(avatarUrl = result))
                }
            }

            // Eliminar la foto actual. 204 siempre (idempotente).
            delete {
                service.delete(call.userId())
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
