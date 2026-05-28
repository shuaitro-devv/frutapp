package cl.frutapp.backend.plugins

import cl.frutapp.backend.error.ApiException
import cl.frutapp.shared.dto.ApiError
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import org.slf4j.LoggerFactory

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
        exception<Throwable> { call, cause ->
            logger.error("Error no manejado", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ApiError("internal_error", "Ocurrió un error inesperado. Intenta de nuevo más tarde.")
            )
        }
    }
}
