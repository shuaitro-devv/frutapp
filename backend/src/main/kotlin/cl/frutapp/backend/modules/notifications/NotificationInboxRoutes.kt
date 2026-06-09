package cl.frutapp.backend.modules.notifications

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.NotificationsResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.util.UUID

fun Route.notificationInboxRoutes(repo: NotificationInboxRepository) {
    authenticate(JWT_AUTH) {
        route("/v1/notifications") {
            // Lista las notifs del user actual + count de no leidas en una sola
            // respuesta (asi el cliente no hace 2 round-trips para el badge).
            get {
                val uid = call.userId()
                val items = repo.listByUser(uid)
                val unread = items.count { it.readAt == null }
                call.respond(NotificationsResponse(items = items, unreadCount = unread))
            }

            // Marca todas leidas. Devuelve 204 sin body — el cliente refresca despues.
            post("/read-all") {
                val uid = call.userId()
                repo.markAllReadFor(uid)
                call.respond(HttpStatusCode.NoContent)
            }

            // Marca una sola leida. 404 si no existe o no es del user.
            post("/{id}/read") {
                val uid = call.userId()
                val idStr = call.parameters["id"] ?: throw ValidationException("id requerido")
                val notifId = runCatching { UUID.fromString(idStr) }.getOrNull()
                    ?: throw ValidationException("id invalido")
                val ok = repo.markRead(notifId, uid)
                if (!ok) throw NotFoundException("Notificacion no encontrada.")
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
