package cl.frutapp.backend.modules.referrals

import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.shared.dto.ReferralVerifyResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Endpoints publicos del programa de referidos.
 *
 * `GET /v1/referrals/verify/{codigo}`
 * - Publico (sin auth). Lo consume la landing SSR `/invita/[codigo]` para
 *   validar el codigo y renderizar OG tags con el nombre del referidor.
 * - Rate-limited en el pool "auth" (10 req/min/IP) para evitar enumeracion
 *   de codigos.
 * - Nunca expone email, id ni apellido — solo el primer nombre.
 * - Codigo inexistente devuelve 404 sin distinguir de "existe pero
 *   soft-deleted" (para no filtrar existencia via 410 vs 404).
 */
fun Route.referralRoutes(users: UserRepository) {
    rateLimit(RateLimitName("auth")) {
        route("/v1/referrals") {
            get("/verify/{codigo}") {
                val codigoRaw = call.parameters["codigo"].orEmpty()
                val codigo = codigoRaw.trim().uppercase()
                if (codigo.length !in 6..12 || !codigo.all { it.isLetterOrDigit() }) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                val referidor = users.findByCodigoInvitacion(codigo)
                if (referidor == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                val primerNombre = referidor.name.trim().substringBefore(' ').ifBlank { "Un amigo" }
                call.respond(
                    ReferralVerifyResponse(
                        codigo = codigo,
                        referrerFirstName = primerNombre,
                    )
                )
            }
        }
    }
}
