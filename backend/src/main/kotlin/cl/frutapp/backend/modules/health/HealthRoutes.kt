package cl.frutapp.backend.modules.health

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.domain.HealthResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.slf4j.LoggerFactory

private const val VERSION = "0.1.0"
private val log = LoggerFactory.getLogger("HealthRoutes")

/**
 * Endpoint pensado para keep-alive externo (UptimeRobot). Toca la BD con un `SELECT 1`
 * minimal (sin schema ni tablas — funciona en cualquier estado del backend) para que el
 * cron mantenga viva la pool de HikariCP, no solo Traefik. Si la BD falla → 503 'degraded'
 * y loggea el error (sirve también de smoke test que detecta caídas reales).
 */
fun Route.healthRoutes() {
    route("/v1") {
        get("/health") {
            val dbOk = runCatching {
                dbQuery {
                    TransactionManager.current().exec("SELECT 1") { rs -> rs.next() }
                }
            }.onFailure { log.warn("Health probe DB falló: ${it::class.simpleName}: ${it.message}") }
                .isSuccess
            val resp = HealthResponse(
                status = if (dbOk) "ok" else "degraded",
                version = VERSION,
                timestampMs = System.currentTimeMillis()
            )
            if (dbOk) call.respond(resp) else call.respond(HttpStatusCode.ServiceUnavailable, resp)
        }
    }
}
