package cl.frutapp.backend.plugins

import cl.frutapp.backend.modules.auth.AuthService
import cl.frutapp.backend.modules.auth.authRoutes
import cl.frutapp.backend.modules.catalog.CatalogService
import cl.frutapp.backend.modules.catalog.catalogRoutes
import cl.frutapp.backend.modules.health.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting(authService: AuthService, catalogService: CatalogService) {
    routing {
        healthRoutes()
        authRoutes(authService)
        catalogRoutes(catalogService)
    }
}
