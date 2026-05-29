package cl.frutapp.backend.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.defaultheaders.*
import org.slf4j.event.Level
import java.util.UUID

/**
 * Observabilidad del backend.
 *
 * - **CallId**: genera (o toma del header X-Request-Id) un `traceId` por request, lo
 *   expone en el MDC y lo devuelve en el header `X-Trace-Id`. Así cada request es
 *   correlacionable: el cliente que reporta un error puede dar ese id y se ubica en los logs.
 * - **CallLogging**: loguea cada request (método/ruta/estado) con su `traceId`.
 *
 * El `traceId` aparece en cada línea de log gracias al patrón `%X{traceId}` (ver logback.xml).
 */
fun Application.configureMonitoring() {
    install(DefaultHeaders) {
        header("X-Powered-By", "FrutApp/Ktor")
    }

    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString().substring(0, 8) }
        verify { it.isNotBlank() }
        replyToHeader("X-Trace-Id")
    }

    install(CallLogging) {
        level = Level.INFO
        callIdMdc("traceId")
        // Logs de health/estáticos no aportan; evita ruido.
        filter { call -> !call.request.local.uri.startsWith("/v1/health") }
    }
}
