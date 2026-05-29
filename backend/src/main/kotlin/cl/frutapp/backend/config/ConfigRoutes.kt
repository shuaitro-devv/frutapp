package cl.frutapp.backend.config

import cl.frutapp.shared.dto.AppConfigDto
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

/** Config pública: el cliente la lee al iniciar (envío gratis desde, etc.). Escritura
 *  (back office) queda para cuando exista RBAC; por ahora se edita la tabla app_config. */
fun Route.configRoutes() {
    get("/v1/config") {
        call.respond(AppConfigDto(ConfigCache.clientVisible()))
    }
}
