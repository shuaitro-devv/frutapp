package cl.frutapp.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.defaultheaders.*

/**
 * Headers globales que devuelve el backend.
 *
 * NOTA: CallLogging (request/response logs) se agrega en Sprint 1 cuando se incorpora la
 * configuración completa de observabilidad. Por ahora dejamos solo headers para mantener el
 * Sprint 0 mínimo y verificable.
 */
fun Application.configureMonitoring() {
    install(DefaultHeaders) {
        header("X-Powered-By", "FrutApp/Ktor")
    }
}
