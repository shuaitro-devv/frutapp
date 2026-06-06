package cl.frutapp.backend.modules.audit

import cl.frutapp.backend.error.UnauthorizedException
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.header
import io.ktor.server.request.userAgent
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/**
 * Extrae el [EventContext] desde headers de la request:
 *  - IP real desde Traefik (X-Forwarded-For maneja la cadena de proxies).
 *  - Headers tipicos de la app movil (`X-App-Version`, `X-Device-Model`, etc).
 */
fun ApplicationCall.eventContext(): EventContext = EventContext(
    ipAddress = request.origin.remoteHost,
    userAgent = request.userAgent(),
    appVersion = request.header("X-App-Version"),
    deviceModel = request.header("X-Device-Model"),
    osVersion = request.header("X-Os-Version"),
    locale = request.header("X-Locale") ?: request.headers["Accept-Language"],
    channel = request.header("X-Channel")
)

/**
 * Extrae el UUID del usuario autenticado desde el JWT (claim `sub`). Lanza
 * [UnauthorizedException] si no hay principal o el subject no es UUID valido.
 * Centralizado aca para que un futuro cambio en el shape del JWT toque un solo
 * lugar (antes estaba duplicado en OrderRoutes y StaffOrderRoutes).
 */
fun ApplicationCall.userId(): UUID {
    val sub = principal<JWTPrincipal>()?.subject ?: throw UnauthorizedException()
    return runCatching { UUID.fromString(sub) }.getOrNull() ?: throw UnauthorizedException()
}

/** Helper para armar payload JSON tipado para eventos. Omite claves con valor null
 *  para no confundir 'campo ausente' con 'campo vacio' al hacer queries sobre payload
 *  (ej. `payload->>'motivo' IS NULL` debe distinguir realmente). */
fun jsonOf(vararg pairs: Pair<String, String?>): JsonElement {
    val map = pairs.mapNotNull { (k, v) -> v?.let { k to JsonPrimitive(it) } }.toMap()
    return JsonObject(map)
}
