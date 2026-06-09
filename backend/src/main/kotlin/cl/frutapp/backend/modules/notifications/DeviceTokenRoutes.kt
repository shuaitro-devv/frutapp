package cl.frutapp.backend.modules.notifications

import cl.frutapp.backend.modules.audit.userId
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.DeleteDeviceTokenRequest
import cl.frutapp.shared.dto.RegisterDeviceTokenRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.deviceTokenRoutes(repo: DeviceTokenRepository) {
    authenticate(JWT_AUTH) {
        route("/v1/device/token") {
            post {
                val req = call.receive<RegisterDeviceTokenRequest>()
                val platform = req.platform.uppercase()
                require(platform in PLATFORMS) { "platform inválido: $platform" }
                require(req.fcmToken.isNotBlank()) { "fcmToken vacío" }
                repo.upsert(
                    userId = call.userId(),
                    fcmToken = req.fcmToken,
                    platform = platform,
                    appId = req.appId
                )
                call.respond(HttpStatusCode.NoContent)
            }
            delete {
                val req = call.receive<DeleteDeviceTokenRequest>()
                repo.deleteByToken(req.fcmToken)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private val PLATFORMS = setOf("ANDROID", "IOS", "WEB")
