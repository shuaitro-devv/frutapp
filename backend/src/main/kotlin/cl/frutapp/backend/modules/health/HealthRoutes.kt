package cl.frutapp.backend.modules.health

import cl.frutapp.shared.domain.HealthResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private const val VERSION = "0.1.0"

fun Route.healthRoutes() {
    route("/v1") {
        get("/health") {
            call.respond(
                HealthResponse(
                    status = "ok",
                    version = VERSION,
                    timestampMs = System.currentTimeMillis()
                )
            )
        }
    }
}
