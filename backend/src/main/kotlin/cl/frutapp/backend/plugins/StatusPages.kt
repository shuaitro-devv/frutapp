package cl.frutapp.backend.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

@Serializable
private data class ErrorBody(
    val title: String,
    val status: Int,
    val detail: String? = null,
    val requestId: String? = null
)

fun Application.configureStatusPages() {
    val logger = LoggerFactory.getLogger("StatusPages")

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("Error no manejado", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorBody(
                    title = "Error interno",
                    status = 500,
                    detail = "Ocurrió un error inesperado. Intenta de nuevo más tarde."
                )
            )
        }
    }
}
