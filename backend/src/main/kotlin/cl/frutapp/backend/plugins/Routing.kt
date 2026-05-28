package cl.frutapp.backend.plugins

import cl.frutapp.backend.modules.health.healthRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        healthRoutes()
    }
}
