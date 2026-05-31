package cl.frutapp.backend.modules.health

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.domain.HealthResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll

private const val VERSION = "0.1.0"

/**
 * Endpoint pensado para keep-alive externo (UptimeRobot). Toca la BD para que el cron
 * mantenga viva la pool de HikariCP, no solo Traefik. Si la BD falla → 503 con el
 * detalle (sirve también de smoke test).
 */
fun Route.healthRoutes() {
    route("/v1") {
        get("/health") {
            val dbOk = runCatching {
                dbQuery { HealthProbe.selectAll().limit(1).count() }
            }.isSuccess
            val resp = HealthResponse(
                status = if (dbOk) "ok" else "degraded",
                version = VERSION,
                timestampMs = System.currentTimeMillis()
            )
            if (dbOk) call.respond(resp) else call.respond(HttpStatusCode.ServiceUnavailable, resp)
        }
    }
}

/** Tabla referencia para el probe — usamos flyway_schema_history que crea Flyway. */
private object HealthProbe : Table("flyway_schema_history")
