package cl.frutapp.backend.modules.catalog

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.SetProductAvailabilityRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.catalogRoutes(service: CatalogService) {
    route("/v1") {
        get("/categories") {
            call.respond(service.categories())
        }
        get("/products") {
            val category = call.request.queryParameters["category"]
            val query = call.request.queryParameters["q"]
            call.respond(service.products(category, query))
        }
        get("/products/{id}") {
            val product = service.product(call.parameters["id"])
                ?: throw NotFoundException("Producto no encontrado.")
            call.respond(product)
        }
    }

    // Back office: flipear disponibilidad operacional del producto sin redeploy.
    // Gated por `catalog:write` (sembrado en V19; admin lo tiene).
    authenticate(JWT_AUTH) {
        put("/v1/admin/products/{id}/availability") {
            if (!call.hasPermission("catalog:write")) {
                call.respond(HttpStatusCode.Forbidden); return@put
            }
            val id = call.parameters["id"].orEmpty()
            val body = call.receive<SetProductAvailabilityRequest>()
            call.respond(service.setAvailability(id, body.disponible))
        }

        // Picker pide productos similares para mostrar alternativas reales en el
        // SustitucionModal. Gated por `order:pick` (no necesita catalog:write).
        get("/v1/staff/products/similar") {
            if (!call.hasPermission("order:pick")) {
                call.respond(HttpStatusCode.Forbidden); return@get
            }
            val productIdRaw = call.request.queryParameters["productId"].orEmpty()
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 25) ?: 10
            call.respond(service.similares(productIdRaw, limit))
        }
    }
}
