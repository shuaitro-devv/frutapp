package cl.frutapp.backend.plugins

import cl.frutapp.backend.error.ApiException
import cl.frutapp.shared.dto.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException as KtorNotFoundException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory
import java.util.UUID

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("StatusPages")

    install(StatusPages) {
        // Errores de dominio: cada uno define su status + código estable.
        exception<ApiException> { call, cause ->
            call.respond(cause.statusCode, ApiError(cause.errorCode, cause.message))
        }
        // Body malformado / faltante al deserializar.
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("bad_request", cause.message ?: "Solicitud inválida.")
            )
        }
        // JSON con tipos/structures invalidos. Ktor a veces los envuelve como JsonConvertException
        // (kotlinx.serialization) en lugar de BadRequestException → caian al 500 generico.
        exception<JsonConvertException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("bad_request", "El cuerpo de la solicitud no es válido.")
            )
        }
        // 404 de Ktor (path no matcheado) — devolvemos JSON consistente en lugar de HTML.
        exception<KtorNotFoundException> { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ApiError("not_found", "Recurso no encontrado.")
            )
        }
        // Argumento ilegal: la mayoria de las veces es input del cliente fuera de rango
        // (UUID malformado, password muy larga para bcrypt, numero negativo, etc.).
        // Antes caia al 500 generico; ahora 400 con mensaje claro.
        exception<IllegalArgumentException> { call, cause ->
            logger.warn("Bad argument: {}", cause.message)
            call.respond(
                HttpStatusCode.BadRequest,
                ApiError("bad_request", mensajeBadArgument(cause.message))
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Error no manejado", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("internal_error", "Ocurrió un error inesperado. Intenta de nuevo más tarde.")
            )
        }
    }
}

/** Mapeo conservador de mensajes tecnicos a algo cliente-friendly. Si el caller no
 *  matchea ningun patron, devolvemos un generico para no filtrar detalles internos. */
private fun mensajeBadArgument(raw: String?): String {
    val msg = raw.orEmpty().lowercase()
    return when {
        msg.contains("password") && msg.contains("72") ->
            "La contraseña es demasiado larga. Usá hasta 72 caracteres."
        msg.contains("invalid uuid") || msg.contains("uuid") ->
            "El identificador no es válido."
        else -> "Solicitud inválida."
    }
}

/** Helper para validar UUIDs en rutas y devolver 400 (no 500) si vienen rotos. */
fun parsearUuid(raw: String?, campo: String = "id"): UUID =
    try { UUID.fromString(raw ?: throw IllegalArgumentException("$campo es obligatorio")) }
    catch (_: IllegalArgumentException) { throw IllegalArgumentException("invalid uuid para $campo") }
