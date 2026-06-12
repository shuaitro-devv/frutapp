package cl.frutapp.backend.config

import cl.frutapp.backend.modules.rbac.hasPermission
import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.AdminConfigListResponseDto
import cl.frutapp.shared.dto.UpdateConfigRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

/**
 * Back office de configuración. GET lista todas las entradas (incluyendo no client-visible
 * como llaves internas) para que el panel arme la UI; PUT edita una key existente y
 * fuerza un refresh inmediato del caché para que el cambio surta efecto sin esperar
 * el tick periódico (60s).
 *
 * El public `GET /v1/config` (sin auth, solo client_visible) NO se toca: la app sigue
 * leyendo de ahí.
 */
fun Route.adminConfigRoutes(service: ConfigService, repo: ConfigRepository) {
    authenticate(JWT_AUTH) {
        route("/v1/admin/config") {
            get {
                if (!call.hasPermission("config:read")) {
                    call.respond(HttpStatusCode.Forbidden); return@get
                }
                call.respond(AdminConfigListResponseDto(service.listAll()))
            }
            put("/{key}") {
                if (!call.hasPermission("config:write")) {
                    call.respond(HttpStatusCode.Forbidden); return@put
                }
                val key = call.parameters["key"].orEmpty()
                val body = call.receive<UpdateConfigRequest>()
                val updated = service.update(key, body.value)
                // Refresh inmediato: sin esto el operador edita y la app/backend siguen
                // viendo el valor viejo hasta el próximo tick de 60s. Mejor pagar una
                // query extra acá que ofrecer una UX confusa.
                // En runCatching para no romper la respuesta si el refresh falla (DB
                // lenta, pool exhausted): el UPDATE ya esta en BD, no queremos que el
                // operador vea 500 cuando su edicion fue exitosa. El tick periodico
                // de 60s lo va a alcanzar igual.
                runCatching { ConfigCache.refresh(repo) }
                call.respond(updated)
            }
        }
    }
}
