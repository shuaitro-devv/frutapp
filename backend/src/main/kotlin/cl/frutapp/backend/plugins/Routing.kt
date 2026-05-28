package cl.frutapp.backend.plugins

import cl.frutapp.backend.modules.health.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        healthRoutes()
    }
}
