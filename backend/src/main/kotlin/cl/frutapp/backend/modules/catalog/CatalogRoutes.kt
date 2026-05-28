package cl.frutapp.backend.modules.catalog

import cl.frutapp.backend.error.NotFoundException
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
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
}
